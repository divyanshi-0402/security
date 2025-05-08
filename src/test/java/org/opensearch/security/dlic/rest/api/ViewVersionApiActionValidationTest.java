package org.opensearch.security.dlic.rest.api;

import org.junit.Test;
import org.opensearch.core.rest.RestStatus;
import org.opensearch.security.configuration.SecurityConfigVersionsLoader;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.IOException;

import org.junit.Before;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.opensearch.common.settings.Settings;
import static org.opensearch.security.support.ConfigConstants.SECURITY_CONFIG_VERSION_INDEX_ENABLED;


public class ViewVersionApiActionValidationTest extends AbstractApiActionValidationTest {

    private SecurityConfigVersionsLoader versionsLoader;

    @Before
    public void setUp() {
        versionsLoader = mock(SecurityConfigVersionsLoader.class);

        Settings settings = Settings.builder()
        .put(SECURITY_CONFIG_VERSION_INDEX_ENABLED, true)
        .build();

        securityApiDependencies = new SecurityApiDependencies(
            null,
            configurationRepository,
            null,
            null,
            restApiAdminPrivilegesEvaluator,
            null,
            settings
        );
    }
    @Test
    public void allowsGetRequestOnConfigLoad() throws IOException {
        var validator = new ViewVersionApiAction(clusterService, threadPool, securityApiDependencies, versionsLoader)
            .createEndpointValidator();

        var result = validator.onConfigLoad(SecurityConfiguration.of("some_id", configuration));

        assertTrue(result.isValid());
        assertThat(result.status(), is(RestStatus.OK));
    }

    @Test
    public void forbidsDeleteRequest() throws IOException {
        var validator = new ViewVersionApiAction(clusterService, threadPool, securityApiDependencies, versionsLoader)
            .createEndpointValidator();

        var result = validator.onConfigDelete(SecurityConfiguration.of("some_id", configuration));

        assertFalse(result.isValid());
        assertThat(result.status(), is(RestStatus.FORBIDDEN));
    }

    @Test
    public void forbidsConfigChangeRequest() throws IOException {
        var validator = new ViewVersionApiAction(clusterService, threadPool, securityApiDependencies, versionsLoader)
            .createEndpointValidator();

        var result = validator.onConfigChange(SecurityConfiguration.of("some_id", configuration));

        assertFalse(result.isValid());
        assertThat(result.status(), is(RestStatus.FORBIDDEN));
    }
}