package net.statemesh.k8s.util;

import io.kubernetes.client.Exec;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.*;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import net.statemesh.k8s.api.AimClient;
import net.statemesh.k8s.api.KubeRayClient;
import net.statemesh.k8s.crd.helm.models.V1HelmChart;
import net.statemesh.k8s.crd.helm.models.V1HelmChartList;
import net.statemesh.k8s.crd.postgresql.models.V1Postgresql;
import net.statemesh.k8s.crd.postgresql.models.V1PostgresqlList;
import net.statemesh.k8s.crd.raycluster.models.V1RayCluster;
import net.statemesh.k8s.crd.raycluster.models.V1RayClusterList;
import net.statemesh.k8s.crd.rayjob.models.V1RayJob;
import net.statemesh.k8s.crd.rayjob.models.V1RayJobList;
import net.statemesh.k8s.crd.rayservice.models.V1RayService;
import net.statemesh.k8s.crd.rayservice.models.V1RayServiceList;
import net.statemesh.k8s.crd.tekton.models.*;
import net.statemesh.k8s.crd.traefik.models.*;
import net.statemesh.k8s.api.PrometheusClient;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.ZoneDTO;
import org.springframework.core.env.Environment;

import java.util.Map;

import static org.mockito.Mockito.mock;

/**
 * Factory + fluent builder to create a fully mocked ApiStub instance for unit tests.
 * Only uses the package-private testing constructor of ApiStub so no real Kubernetes calls are ever made.
 */
public final class ApiStubTestFactory {

    private ApiStubTestFactory() {}

    public static ApiStubBuilder builder() { return new ApiStubBuilder(); }

    public static class ApiStubBuilder {
        private ClusterDTO cluster = defaultCluster();
        private ApiClient apiClient = mock(ApiClient.class);
        private CoreV1Api coreV1Api = mock(CoreV1Api.class);
        private AppsV1Api appsV1Api = mock(AppsV1Api.class);
        private RbacAuthorizationV1Api rbacAuthorizationV1Api = mock(RbacAuthorizationV1Api.class);
        private NetworkingV1Api networkingV1Api = mock(NetworkingV1Api.class);
        private Metrics metrics = mock(Metrics.class);
        private PodLogs podLogs = mock(PodLogs.class);
        private Exec exec = mock(Exec.class);
        private CustomObjectsApi customObjectsApi = mock(CustomObjectsApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1alpha1IngressRoute, V1alpha1IngressRouteList> traefikIngress = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1alpha1IngressRouteTCP, V1alpha1IngressRouteTCPList> traefikTCPIngress = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1alpha1Middleware, V1alpha1MiddlewareList> traefikMiddleware = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1alpha1MiddlewareTCP, V1alpha1MiddlewareTCPList> traefikMiddlewareTCP = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1HelmChart, V1HelmChartList> helmController = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1beta1PipelineRun, V1beta1PipelineRunList> pipelineRun = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1Postgresql, V1PostgresqlList> postgreSQL = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1TaskRun, V1TaskRunList> taskRun = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1RayCluster, V1RayClusterList> rayCluster = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1RayService, V1RayServiceList> rayService = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private GenericKubernetesApi<V1RayJob, V1RayJobList> rayJob = mock(GenericKubernetesApi.class);
        @SuppressWarnings({"rawtypes", "unchecked"})
        private PrometheusClient prometheusClient = mock(PrometheusClient.class);
        private Map<String, KubeRayClient> kubeRayClients = mock(Map.class);
        private Map<String, AimClient> aimClients = mock(Map.class);
        private Environment environment = mock(Environment.class);

        public ApiStubBuilder cluster(ClusterDTO cluster) { this.cluster = cluster; return this; }
        public ApiStubBuilder apiClient(ApiClient v) { this.apiClient = v; return this; }
        public ApiStubBuilder coreV1Api(CoreV1Api v) { this.coreV1Api = v; return this; }
        public ApiStubBuilder appsV1Api(AppsV1Api v) { this.appsV1Api = v; return this; }
        public ApiStubBuilder rbacAuthorizationV1Api(RbacAuthorizationV1Api v) { this.rbacAuthorizationV1Api = v; return this; }
        public ApiStubBuilder networkingV1Api(NetworkingV1Api v) { this.networkingV1Api = v; return this; }
        public ApiStubBuilder metrics(Metrics v) { this.metrics = v; return this; }
        public ApiStubBuilder podLogs(PodLogs v) { this.podLogs = v; return this; }
        public ApiStubBuilder exec(Exec v) { this.exec = v; return this; }
        public ApiStubBuilder customObjectsApi(CustomObjectsApi v) { this.customObjectsApi = v; return this; }
        public ApiStubBuilder traefikIngress(GenericKubernetesApi<V1alpha1IngressRoute, V1alpha1IngressRouteList> v) { this.traefikIngress = v; return this; }
        public ApiStubBuilder traefikTCPIngress(GenericKubernetesApi<V1alpha1IngressRouteTCP, V1alpha1IngressRouteTCPList> v) { this.traefikTCPIngress = v; return this; }
        public ApiStubBuilder traefikMiddleware(GenericKubernetesApi<V1alpha1Middleware, V1alpha1MiddlewareList> v) { this.traefikMiddleware = v; return this; }
        public ApiStubBuilder traefikMiddlewareTCP(GenericKubernetesApi<V1alpha1MiddlewareTCP, V1alpha1MiddlewareTCPList> v) { this.traefikMiddlewareTCP = v; return this; }
        public ApiStubBuilder helmController(GenericKubernetesApi<V1HelmChart, V1HelmChartList> v) { this.helmController = v; return this; }
        public ApiStubBuilder pipelineRun(GenericKubernetesApi<V1beta1PipelineRun, V1beta1PipelineRunList> v) { this.pipelineRun = v; return this; }
        public ApiStubBuilder postgreSQL(GenericKubernetesApi<V1Postgresql, V1PostgresqlList> v) { this.postgreSQL = v; return this; }
        public ApiStubBuilder taskRun(GenericKubernetesApi<V1TaskRun, V1TaskRunList> v) { this.taskRun = v; return this; }
        public ApiStubBuilder rayCluster(GenericKubernetesApi<V1RayCluster, V1RayClusterList> v) { this.rayCluster = v; return this; }
        public ApiStubBuilder rayService(GenericKubernetesApi<V1RayService, V1RayServiceList> v) { this.rayService = v; return this; }
        public ApiStubBuilder rayJob(GenericKubernetesApi<V1RayJob, V1RayJobList> v) { this.rayJob = v; return this; }
        public ApiStubBuilder prometheusClient(PrometheusClient v) { this.prometheusClient = v; return this; }
        public ApiStubBuilder kubeRayClients(Map<String, KubeRayClient> v) { this.kubeRayClients = v; return this; }
        public ApiStubBuilder aimClients(Map<String, AimClient> v) { this.aimClients = v; return this; }
        public ApiStubBuilder environment(Environment v) { this.environment = v; return this; }

        public ApiStub build() {
            return new ApiStub(
                cluster,
                apiClient,
                coreV1Api,
                appsV1Api,
                rbacAuthorizationV1Api,
                networkingV1Api,
                metrics,
                podLogs,
                exec,
                customObjectsApi,
                traefikIngress,
                traefikTCPIngress,
                traefikMiddleware,
                traefikMiddlewareTCP,
                helmController,
                pipelineRun,
                postgreSQL,
                taskRun,
                rayCluster,
                rayService,
                rayJob,
                prometheusClient,
                kubeRayClients,
                aimClients,
                environment
            );
        }

        private static ClusterDTO defaultCluster() {
            ClusterDTO c = new ClusterDTO();
            c.setId("cluster-test");
            c.setName("cluster-test-name");
            c.setCid("cid-test");
            c.setMasterIp("127.0.0.1");
            c.setVpnAuth("dummy");
            c.setKubeConfig("apiVersion: v1\nclusters: []\n");
            ZoneDTO zone = new ZoneDTO();
            zone.setId("zone-id");
            zone.setName("zone-name");
            zone.setZoneId("zone-test");
            zone.setVpnApiKey("vpn-key");
            zone.setIperfIp("127.0.0.1");
            c.setZone(zone);
            return c;
        }
    }
}
