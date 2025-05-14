package org.opensearch.security.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opensearch.common.settings.Settings;
import org.opensearch.common.util.concurrent.ThreadContext;
import org.opensearch.security.configuration.SecurityConfigVersionDocument.Version;
import org.opensearch.security.securityconf.DynamicConfigFactory;
import org.opensearch.security.support.ConfigConstants;
import org.opensearch.security.user.User;

import org.greenrobot.eventbus.Subscribe;

public class ConfigVersionInitializer {

    private static final Logger log = LogManager.getLogger(ConfigVersionInitializer.class);

    private final ConfigurationRepository cr;
    private final Settings settings;
    private final ThreadContext threadContext;

    public ConfigVersionInitializer(ConfigurationRepository cr, Settings settings, ThreadContext threadContext) {
        this.cr = cr;
        this.settings = settings;
        this.threadContext = threadContext;
    }

    @Subscribe
    public void onConfigInitialized(DynamicConfigFactory.ConfigInitializedEvent event) {
        if (!ConfigurationRepository.isVersionIndexEnabled(settings)) return;

        try {
            log.info("Initializing version index (.opendistro_security_config_versions)");

            if (!cr.createOpendistroSecurityConfigVersionsIndexIfAbsent()) {
                log.info("Version index already exists, skipping initialization.");
                return;
            }

            cr.waitForOpendistroSecurityConfigVersionsIndexToBeAtLeastYellow();

            String nextVersionId = cr.fetchNextVersionId();
            User user = threadContext.getTransient(ConfigConstants.OPENDISTRO_SECURITY_USER);
            String userinfo = (user != null) ? user.getName() : ("v1".equals(nextVersionId) ? "system" : "unknown");

            Version<?> version = cr.buildVersionFromSecurityIndex(nextVersionId, userinfo);
            cr.saveCurrentVersionToSystemIndex(version);

        } catch (Exception e) {
            log.error("Failed to initialize config version index", e);
        }
    }
}
