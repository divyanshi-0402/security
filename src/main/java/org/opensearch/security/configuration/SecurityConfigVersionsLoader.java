package org.opensearch.security.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.action.get.GetRequest;
import org.opensearch.action.get.GetResponse;
import org.opensearch.client.Client;
import org.opensearch.common.settings.Settings;
import org.opensearch.core.action.ActionListener;
import org.opensearch.security.DefaultObjectMapper;
import org.opensearch.security.support.ConfigConstants;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SecurityConfigVersionsLoader {
    private static final Logger log = LogManager.getLogger(SecurityConfigVersionsLoader.class);

    private final Client client;
    private final String opendistroSecurityConfigVersionsIndex;

    public SecurityConfigVersionsLoader(Client client, Settings settings) {
        this.client = client;
        this.opendistroSecurityConfigVersionsIndex = settings.get(
            ConfigConstants.SECURITY_CONFIG_VERSIONS_INDEX_NAME,
            ConfigConstants.OPENDISTRO_SECURITY_CONFIG_VERSIONS_INDEX
        );
    }

    public void loadLatestVersionAsync(ActionListener<SecurityConfigVersionDocument.Version> listener) {
        GetRequest getRequest = new GetRequest(opendistroSecurityConfigVersionsIndex, "opendistro_security_config_versions");

        client.get(getRequest, new ActionListener<>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                try {
                    if (!getResponse.isExists()) {
                        log.warn("Config versions document not found in {}", opendistroSecurityConfigVersionsIndex);
                        listener.onResponse(null);
                        return;
                    }

                    SecurityConfigVersionDocument doc = DefaultObjectMapper.readValue(getResponse.getSourceAsString(), SecurityConfigVersionDocument.class);
                    List<SecurityConfigVersionDocument.Version> versions = doc.getVersions();
                    
                    doc.setSeqNo(getResponse.getSeqNo());
                    doc.setPrimaryTerm(getResponse.getPrimaryTerm());

                    if (versions == null || versions.isEmpty()) {
                        listener.onResponse(null);
                    } else {
                        sortVersionsById(versions);
                        listener.onResponse(versions.get(versions.size() - 1)); // latest
                    }
                } catch (IOException e) {
                    log.error("Failed to parse config versions doc", e);
                    listener.onFailure(e);
                }
            }

            @Override
            public void onFailure(Exception e) {
                log.error("Failed to load config versions doc from {}", opendistroSecurityConfigVersionsIndex, e);
                listener.onFailure(e);
            }
        });
    }

    public SecurityConfigVersionDocument.Version loadLatestVersion() {
        CountDownLatch latch = new CountDownLatch(1);
        final SecurityConfigVersionDocument.Version[] result = new SecurityConfigVersionDocument.Version[1];
        final Exception[] failure = new Exception[1];
    
        loadLatestVersionAsync(new ActionListener<>() {
            @Override
            public void onResponse(SecurityConfigVersionDocument.Version version) {
                result[0] = version;
                latch.countDown();
            }
    
            @Override
            public void onFailure(Exception e) {
                failure[0] = e;
                latch.countDown();
            }
        });
    
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout waiting for loadLatestVersionAsync()");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for config version load", e);
        }
    
        if (failure[0] != null) {
            throw new RuntimeException("Failed to load latest config version", failure[0]);
        }
    
        return result[0];
    }

    public void loadFullDocumentAsync(ActionListener<SecurityConfigVersionDocument> listener) {
        GetRequest getRequest = new GetRequest(opendistroSecurityConfigVersionsIndex, "opendistro_security_config_versions");
    
        client.get(getRequest, new ActionListener<>() {
            @Override
            public void onResponse(GetResponse getResponse) {
                try {
                    if (!getResponse.isExists()) {
                        log.warn("Config versions document not found in {}", opendistroSecurityConfigVersionsIndex);
                        listener.onResponse(new SecurityConfigVersionDocument()); // return empty doc
                        return;
                    }
    
                    SecurityConfigVersionDocument doc = DefaultObjectMapper.readValue(
                        getResponse.getSourceAsString(),
                        SecurityConfigVersionDocument.class
                    );

                    doc.setSeqNo(getResponse.getSeqNo());
                    doc.setPrimaryTerm(getResponse.getPrimaryTerm());

                    listener.onResponse(doc);
                } catch (IOException e) {
                    log.error("Failed to parse config versions doc", e);
                    listener.onFailure(e);
                }
            }
    
            @Override
            public void onFailure(Exception e) {
                log.error("Failed to load config versions doc from {}", opendistroSecurityConfigVersionsIndex, e);
                listener.onFailure(e);
            }
        });
    }
    

    public SecurityConfigVersionDocument loadFullDocument() {
        final SecurityConfigVersionDocument[] result = new SecurityConfigVersionDocument[1];
        final Exception[] error = new Exception[1];
        final CountDownLatch latch = new CountDownLatch(1);
    
        loadFullDocumentAsync(new ActionListener<>() {
            @Override
            public void onResponse(SecurityConfigVersionDocument doc) {
                result[0] = doc;
                latch.countDown();
            }
    
            @Override
            public void onFailure(Exception e) {
                error[0] = e;
                latch.countDown();
            }
        });
    
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout while loading full config version document");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while loading full config version document", e);
        }
    
        if (error[0] != null) {
            throw new RuntimeException("Failed to load full config version document", error[0]);
        }
    
        return result[0] != null ? result[0] : new SecurityConfigVersionDocument();
    }

    public static void sortVersionsById(List<SecurityConfigVersionDocument.Version> versions) {
        versions.sort((v1, v2) -> {
            try {
                int n1 = Integer.parseInt(v1.getVersion_id().substring(1));
                int n2 = Integer.parseInt(v2.getVersion_id().substring(1));
                return Integer.compare(n1, n2);
            } catch (Exception e) {
                log.warn("Invalid version_id format", e);
                return 0;
            }
        });
    }    
    
}
