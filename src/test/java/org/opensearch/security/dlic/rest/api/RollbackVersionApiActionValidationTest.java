package org.opensearch.security.dlic.rest.api;

import org.junit.Before;
import org.junit.Test;
import org.opensearch.security.configuration.SecurityConfigVersionsLoader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

import java.io.IOException;

public class RollbackVersionApiActionValidationTest extends AbstractApiActionValidationTest {

    private RollbackVersionApiAction rollbackVersionApiAction;

    @Before
    public void setupTest() {
        SecurityConfigVersionsLoader versionsLoader = mock(SecurityConfigVersionsLoader.class);
        rollbackVersionApiAction = new RollbackVersionApiAction(
            clusterService,
            threadPool,
            securityApiDependencies,
            versionsLoader,
            configurationRepository
        );
    }

    @Test
    public void testOnConfigDelete_isForbidden() throws IOException {
        var result = rollbackVersionApiAction.createEndpointValidator()
            .onConfigDelete(SecurityConfiguration.of(null, configuration));
        assertThat(result.status(), is(org.opensearch.core.rest.RestStatus.FORBIDDEN));
    }

    @Test
    public void testOnConfigLoad_isAllowed() throws IOException {
        var result = rollbackVersionApiAction.createEndpointValidator()
            .onConfigLoad(SecurityConfiguration.of(null, configuration));
        assertThat(result.status(), is(org.opensearch.core.rest.RestStatus.OK));
    }

    @Test
    public void testOnConfigChange_isAllowed() throws IOException {
        var result = rollbackVersionApiAction.createEndpointValidator()
            .onConfigChange(SecurityConfiguration.of(null, configuration));
        assertThat(result.status(), is(org.opensearch.core.rest.RestStatus.OK));
    }

}