package net.statemesh.k8s.util;

import io.kubernetes.client.openapi.apis.CoreV1Api;
import net.statemesh.k8s.api.PrometheusClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@DisplayName("ApiStub testing infrastructure smoke tests")
class ApiStubTestFactoryTest {

    @Test
    @DisplayName("Builder creates ApiStub with all mocked dependencies and allows overrides")
    void builderCreatesMockedApiStub() {
        CoreV1Api customCore = mock(CoreV1Api.class);
        PrometheusClient customProm = mock(PrometheusClient.class);

        ApiStub apiStub = ApiStubTestFactory.builder()
            .coreV1Api(customCore)
            .prometheusClient(customProm)
            .build();

        assertNotNull(apiStub.getCluster(), "Cluster should be set");
        assertSame(customCore, apiStub.getCoreV1Api(), "Override CoreV1Api not applied");
        assertSame(customProm, apiStub.getPrometheusClient(), "Override PrometheusClient not applied");
        // Sanity: other fields should be mocks
        assertTrue(Mockito.mockingDetails(apiStub.getAppsV1Api()).isMock());
        assertTrue(Mockito.mockingDetails(apiStub.getRbacAuthorizationV1Api()).isMock());
    }

    @Test
    @ExtendWith(ApiStubParameterResolver.class)
    @DisplayName("ParameterResolver injects mocked ApiStub")
    void parameterResolverInjects(ApiStub apiStub) {
        assertNotNull(apiStub, "Injected ApiStub should not be null");
        assertTrue(Mockito.mockingDetails(apiStub.getCoreV1Api()).isMock());
    }
}

