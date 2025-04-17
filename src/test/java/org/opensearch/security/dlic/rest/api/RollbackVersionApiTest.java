package org.opensearch.security.dlic.rest.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.opensearch.security.test.helper.rest.RestHelper.HttpResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class RollbackVersionApiTest extends AbstractRestApiUnitTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Before
    public void startCluster() throws Exception {
        super.setup();
        rh.sendAdminCertificate = true;

        String docPayload = """
            {
              "versions": [
                {
                  "version_id": "v1",
                  "timestamp": "2025-04-01T00:00:00Z",
                  "modified_by": "admin",
                  "security_configs": {
                    "internalusers": {
                      "lastUpdated": "2025-04-01T00:00:00Z",
                      "configData": {
                        "testuser": {
                          "hash": "$2y$12$dummyHash",
                          "backend_roles": ["role1"]
                        }
                      }
                    }
                  }
                },
                {
                  "version_id": "v2",
                  "timestamp": "2025-04-05T00:00:00Z",
                  "modified_by": "admin",
                  "security_configs": {
                    "internalusers": {
                      "lastUpdated": "2025-04-05T00:00:00Z",
                      "configData": {
                        "testuser": {
                          "hash": "$2y$12$anotherHash",
                          "backend_roles": ["role2"]
                        }
                      }
                    }
                  }
                }
              ]
            }
        """;

        HttpResponse response = rh.executePutRequest(
            "/.opendistro_security_config_versions/_doc/opendistro_security_config_versions",
            docPayload
        );

        assertThat("Failed to insert config versions doc", response.getStatusCode(), is(201));
    }

    @Test
    public void testRollbackToPreviousVersion_success() throws Exception {
        rh.sendAdminCertificate = true;

        HttpResponse response = rh.executePostRequest("/_opendistro/_security/api/rollback", "");

        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        assertThat(response.getBody(), containsString("config rolled back to version v1"));
    }

    @Test
    public void testRollbackToSpecificVersion_success() throws Exception {
        rh.sendAdminCertificate = true;

        HttpResponse response = rh.executePostRequest("/_opendistro/_security/api/rollback/version/v1", "");

        assertThat(response.getStatusCode(), is(HttpStatus.SC_OK));
        assertThat(response.getBody(), containsString("config rolled back to version v1"));
    }

    @Test
    public void testRollbackToInvalidVersion_shouldFail() throws Exception {
        rh.sendAdminCertificate = true;

        HttpResponse response = rh.executePostRequest("/_opendistro/_security/api/rollback/version/invalid", "");

        assertThat(response.getStatusCode(), is(HttpStatus.SC_NOT_FOUND));
        assertThat(response.getBody(), containsString("Version invalid not found"));
    }

    @Test
    public void testRollbackWithoutEnoughVersions_shouldFail() throws Exception {
        rh.sendAdminCertificate = true;

        String singleVersionDoc = """
            {
              "versions": [
                {
                  "version_id": "v1",
                  "timestamp": "2025-04-01T00:00:00Z",
                  "modified_by": "admin",
                  "security_configs": {
                    "internalusers": {
                      "lastUpdated": "2025-04-01T00:00:00Z",
                      "configData": {
                        "testuser": {
                          "hash": "$2y$12$dummyHash",
                          "backend_roles": ["role1"]
                        }
                      }
                    }
                  }
                }
              ]
            }
        """;

        HttpResponse overwrite = rh.executePutRequest(
            "/.opendistro_security_config_versions/_doc/opendistro_security_config_versions",
            singleVersionDoc
        );

        assertThat(overwrite.getStatusCode(), is(200));

        HttpResponse rollback = rh.executePostRequest("/_opendistro/_security/api/rollback", "");
        assertThat(rollback.getStatusCode(), is(HttpStatus.SC_NOT_FOUND));
        assertThat(rollback.getBody(), containsString("No previous version available to rollback"));
    }

    @Test
    public void testRollbackWithoutAdminCert_shouldFail() throws Exception {
        rh.sendAdminCertificate = false;

        HttpResponse response = rh.executePostRequest("/_opendistro/_security/api/rollback", "");

        assertThat(response.getStatusCode(), isOneOf(HttpStatus.SC_UNAUTHORIZED, HttpStatus.SC_FORBIDDEN));
    }
}