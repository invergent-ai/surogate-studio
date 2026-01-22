package net.statemesh.k8s.util;

import io.kubernetes.client.Exec;
import io.kubernetes.client.Metrics;
import io.kubernetes.client.PodLogs;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.*;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import lombok.Getter;
import net.statemesh.k8s.api.AimClient;
import net.statemesh.k8s.api.KubeRayClient;
import net.statemesh.k8s.api.PrometheusClient;
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
import net.statemesh.k8s.crd.tekton.models.V1TaskRun;
import net.statemesh.k8s.crd.tekton.models.V1TaskRunList;
import net.statemesh.k8s.crd.tekton.models.V1beta1PipelineRun;
import net.statemesh.k8s.crd.tekton.models.V1beta1PipelineRunList;
import net.statemesh.k8s.crd.traefik.models.*;
import net.statemesh.service.dto.ClusterDTO;
import okhttp3.Dispatcher;
import okhttp3.Protocol;
import org.springframework.core.env.Environment;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.Executors;

import static net.statemesh.k8s.util.K8SConstants.*;

@Getter
public class ApiStub {
    private final ClusterDTO cluster;
    private final ApiClient apiClient;
    private final CoreV1Api coreV1Api;
    private final AppsV1Api appsV1Api;
    private final RbacAuthorizationV1Api rbacAuthorizationV1Api;
    private final NetworkingV1Api networkingV1Api;
    private final Metrics metrics;
    private final PodLogs logs;
    private final Exec exec;
    private final CustomObjectsApi customApi;
    private final GenericKubernetesApi<V1alpha1IngressRoute, V1alpha1IngressRouteList> traefikIngress;
    private final GenericKubernetesApi<V1alpha1IngressRouteTCP, V1alpha1IngressRouteTCPList> traefikTCPIngress;
    private final GenericKubernetesApi<V1alpha1Middleware, V1alpha1MiddlewareList> traefikMiddleware;
    private final GenericKubernetesApi<V1alpha1MiddlewareTCP, V1alpha1MiddlewareTCPList> traefikMiddlewareTCP;
    private final GenericKubernetesApi<V1HelmChart, V1HelmChartList> helmController;
    private final GenericKubernetesApi<V1beta1PipelineRun, V1beta1PipelineRunList> pipelineRun;
    private final GenericKubernetesApi<V1Postgresql, V1PostgresqlList> postgreSQL;
    private final GenericKubernetesApi<V1TaskRun, V1TaskRunList> taskRun;
    private final GenericKubernetesApi<V1RayCluster, V1RayClusterList> rayCluster;
    private final GenericKubernetesApi<V1RayService, V1RayServiceList> rayService;
    private final GenericKubernetesApi<V1RayJob, V1RayJobList> rayJob;
    private final PrometheusClient prometheusClient;
    private final Map<String, KubeRayClient> kubeRayClients;
    private final Map<String, AimClient> aimClients;
    private final Environment environment;

    public ApiStub(ClusterDTO cluster,
                   ApiClient apiClient,
                   PrometheusClient prometheusClient,
                   Map<String, KubeRayClient> kubeRayClients,
                   Map<String, AimClient> aimClients,
                   Environment environment) {
        this.cluster = cluster;
        this.apiClient = reconfigureApiClient(apiClient);
        this.coreV1Api = new CoreV1Api(apiClient);
        this.appsV1Api = new AppsV1Api(apiClient);
        this.rbacAuthorizationV1Api = new RbacAuthorizationV1Api(apiClient);
        this.networkingV1Api = new NetworkingV1Api(apiClient);
        this.metrics = new Metrics(apiClient);
        this.logs = new PodLogs(apiClient);
        this.exec = new Exec(apiClient);
        this.customApi = new CustomObjectsApi(apiClient);
        this.traefikIngress = new GenericKubernetesApi<>(
            V1alpha1IngressRoute.class,
            V1alpha1IngressRouteList.class,
            TRAEFIK_GROUP,
            TRAEFIK_API_VERSION,
            TRAEFIK_INGRESS_ROUTE_PLURAL,
            apiClient
        );
        this.traefikTCPIngress = new GenericKubernetesApi<>(
            V1alpha1IngressRouteTCP.class,
            V1alpha1IngressRouteTCPList.class,
            TRAEFIK_GROUP,
            TRAEFIK_API_VERSION,
            TRAEFIK_INGRESS_TCP_PLURAL,
            apiClient
        );
        this.traefikMiddleware = new GenericKubernetesApi<>(
            V1alpha1Middleware.class,
            V1alpha1MiddlewareList.class,
            TRAEFIK_GROUP,
            TRAEFIK_API_VERSION,
            TRAEFIK_MIDDLEWARE_PLURAL,
            apiClient
        );
        this.traefikMiddlewareTCP = new GenericKubernetesApi<>(
            V1alpha1MiddlewareTCP.class,
            V1alpha1MiddlewareTCPList.class,
            TRAEFIK_GROUP,
            TRAEFIK_API_VERSION,
            TRAEFIK_MIDDLEWARE_TCP_PLURAL,
            apiClient
        );
        this.helmController = new GenericKubernetesApi<>(
            V1HelmChart.class,
            V1HelmChartList.class,
            HELM_CHART_GROUP,
            HELM_CHART_API_VERSION,
            HELM_CHART_PLURAL,
            apiClient
        );
        this.pipelineRun = new GenericKubernetesApi<>(
            V1beta1PipelineRun.class,
            V1beta1PipelineRunList.class,
            TEKTON_GROUP,
            TEKTON_API_VERSION,
            TEKTON_PIPELINE_RUN_PLURAL,
            apiClient
        );
        this.postgreSQL = new GenericKubernetesApi<>(
            V1Postgresql.class,
            V1PostgresqlList.class,
            POSTGRESQL_GROUP,
            POSTGRESQL_API_VERSION,
            POSTGRESQL_PLURAL,
            apiClient
        );

        this.taskRun = new GenericKubernetesApi<>(
            V1TaskRun.class,
            V1TaskRunList.class,
            TEKTON_GROUP,
            TEKTON_API_VERSION,
            TEKTON_TASK_RUN_PLURAL,
            apiClient
        );
        this.rayCluster = new GenericKubernetesApi<>(
            V1RayCluster.class,
            V1RayClusterList.class,
            RAY_GROUP,
            RAY_CLUSTER_API_VERSION_V1,
            RAY_CLUSTER_PLURAL,
            apiClient
        );
        this.rayService = new GenericKubernetesApi<>(
            V1RayService.class,
            V1RayServiceList.class,
            RAY_GROUP,
            RAY_SERVICE_API_VERSION_V1,
            RAY_SERVICE_PLURAL,
            apiClient
        );
        this.rayJob = new GenericKubernetesApi<>(
            V1RayJob.class,
            V1RayJobList.class,
            RAY_GROUP,
            RAY_JOB_API_VERSION_V1,
            RAY_JOB_PLURAL,
            apiClient
        );
        this.prometheusClient = prometheusClient;
        this.kubeRayClients = kubeRayClients;
        this.aimClients = aimClients;
        this.environment = environment;
    }

    /**
     * Package-private constructor intended ONLY for tests so that all Kubernetes clients can be supplied as mocks.
     * It skips any internal reconfiguration logic and assigns provided instances directly.
     */
    ApiStub(ClusterDTO cluster,
            ApiClient apiClient,
            CoreV1Api coreV1Api,
            AppsV1Api appsV1Api,
            RbacAuthorizationV1Api rbacAuthorizationV1Api,
            NetworkingV1Api networkingV1Api,
            Metrics metrics,
            PodLogs logs,
            Exec exec,
            CustomObjectsApi customApi,
            GenericKubernetesApi<V1alpha1IngressRoute, V1alpha1IngressRouteList> traefikIngress,
            GenericKubernetesApi<V1alpha1IngressRouteTCP, V1alpha1IngressRouteTCPList> traefikTCPIngress,
            GenericKubernetesApi<V1alpha1Middleware, V1alpha1MiddlewareList> traefikMiddleware,
            GenericKubernetesApi<V1alpha1MiddlewareTCP, V1alpha1MiddlewareTCPList> traefikMiddlewareTCP,
            GenericKubernetesApi<V1HelmChart, V1HelmChartList> helmController,
            GenericKubernetesApi<V1beta1PipelineRun, V1beta1PipelineRunList> pipelineRun,
            GenericKubernetesApi<V1Postgresql, V1PostgresqlList> postgreSQL,
            GenericKubernetesApi<V1TaskRun, V1TaskRunList> taskRun,
            GenericKubernetesApi<V1RayCluster, V1RayClusterList> rayCluster,
            GenericKubernetesApi<V1RayService, V1RayServiceList> rayService,
            GenericKubernetesApi<V1RayJob, V1RayJobList> rayJob,
            PrometheusClient prometheusClient,
            Map<String, KubeRayClient> kubeRayClients,
            Map<String, AimClient> aimClients,
            Environment environment) {
        this.cluster = cluster;
        this.apiClient = apiClient; // DO NOT reconfigure for test determinism
        this.coreV1Api = coreV1Api;
        this.appsV1Api = appsV1Api;
        this.rbacAuthorizationV1Api = rbacAuthorizationV1Api;
        this.networkingV1Api = networkingV1Api;
        this.metrics = metrics;
        this.logs = logs;
        this.exec = exec;
        this.customApi = customApi;
        this.traefikIngress = traefikIngress;
        this.traefikTCPIngress = traefikTCPIngress;
        this.traefikMiddleware = traefikMiddleware;
        this.traefikMiddlewareTCP = traefikMiddlewareTCP;
        this.helmController = helmController;
        this.pipelineRun = pipelineRun;
        this.taskRun = taskRun;
        this.rayCluster = rayCluster;
        this.rayService = rayService;
        this.rayJob = rayJob;
        this.postgreSQL = postgreSQL;
        this.prometheusClient = prometheusClient;
        this.kubeRayClients = kubeRayClients;
        this.aimClients = aimClients;
        this.environment = environment;
    }

    private ApiClient reconfigureApiClient(ApiClient apiClient) {
        // Configure k8s client with Virtual Threads
        apiClient.setHttpClient(
            apiClient.getHttpClient().newBuilder()
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))
                .readTimeout(Duration.ZERO)
                .pingInterval(Duration.ofMinutes(1L))
                .dispatcher(new Dispatcher(Executors.newVirtualThreadPerTaskExecutor()))
                .hostnameVerifier((hostname, session) -> true)
                .build()
        );
        return apiClient;
    }
}
