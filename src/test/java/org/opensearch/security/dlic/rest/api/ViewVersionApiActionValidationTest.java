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

public class ViewVersionApiActionValidationTest extends AbstractApiActionValidationTest {

    private SecurityConfigVersionsLoader versionsLoader;

    @Before
    public void setUp() {
        versionsLoader = mock(SecurityConfigVersionsLoader.class);
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