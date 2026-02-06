package net.statemesh.k8s;

import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.CallGeneratorParams;
import io.kubernetes.client.util.Config;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.config.Constants;
import net.statemesh.k8s.api.AimClient;
import net.statemesh.k8s.api.KubeRayClient;
import net.statemesh.k8s.api.PrometheusClient;
import net.statemesh.k8s.api.task.AimMetricsTask;
import net.statemesh.k8s.handler.NodeEventHandler;
import net.statemesh.k8s.strategy.ClusterSelectionFactory;
import net.statemesh.k8s.strategy.ClusterSelectionStrategy;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.task.application.*;
import net.statemesh.k8s.task.application.storage.DeletePersistentVolumeClaimTask;
import net.statemesh.k8s.task.application.storage.DeleteStorageClassTask;
import net.statemesh.k8s.task.application.storage.PersistentVolumeClaimTask;
import net.statemesh.k8s.task.application.storage.StorageClassTask;
import net.statemesh.k8s.task.control.*;
import net.statemesh.k8s.task.db.DeletePostgreSQLTask;
import net.statemesh.k8s.task.db.PostgreSQLTask;
import net.statemesh.k8s.task.ingress.DeleteIngressTCPTask;
import net.statemesh.k8s.task.ingress.DeleteIngressTask;
import net.statemesh.k8s.task.ingress.IngressTCPTask;
import net.statemesh.k8s.task.ingress.IngressTask;
import net.statemesh.k8s.task.misc.PrometheusMetricsTask;
import net.statemesh.k8s.task.network.*;
import net.statemesh.k8s.task.node.AddNodeLabelTask;
import net.statemesh.k8s.task.node.DeleteNodeTask;
import net.statemesh.k8s.task.ray.*;
import net.statemesh.k8s.task.secret.CreateSecretTask;
import net.statemesh.k8s.task.secret.DeleteSecretTask;
import net.statemesh.k8s.task.security.CreateRoleBindingTask;
import net.statemesh.k8s.task.security.CreateRoleTask;
import net.statemesh.k8s.task.security.CreateServiceAccountTask;
import net.statemesh.k8s.task.tekton.*;
import net.statemesh.k8s.util.*;
import net.statemesh.repository.ApplicationRepository;
import net.statemesh.service.*;
import net.statemesh.service.dto.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
@Slf4j
public class KubernetesController {
    private final OrganizationService organizationService;
    @Getter
    private final ClusterService clusterService;
    private final ZoneService zoneService;
    private final RestTemplate restTemplate;
    private final ApplicationRepository applicationRepository;

    private final ClusterSelectionStrategy clusterSelector;
    @Getter
    private final ApplicationProperties applicationProperties;

    private final AsyncTaskExecutor smTaskExecutor;
    @Getter
    private final NodeService nodeService;
    @Getter
    private final SystemConfigurationService systemConfigurationService;
    @Getter
    private final NodeReservationService nodeReservationService;
    @Getter
    private final SimpMessagingTemplate simpMessagingTemplate;
    @Getter
    private final TransactionTemplate transactionTemplate;
    @Getter
    // Map<ZoneId, Map<ClusterId, ApiStub>>
    private final Map<String, Map<String, ApiStub>> clients = new ConcurrentHashMap<>();
    @Getter
    private final Environment environment;
    @Getter
    private ControllerConfig controllerConfig;
    @Getter
    private StorageConfig storageConfig;
    private TaskConfig taskConfig;

    @PostConstruct
    private void initClusterClients() {
        initConfig();

        log.info("Initializing k8s cluster clients");
        final Optional<OrganizationDTO> defaultOrganization =
            organizationService.findOne(Constants.STATE_MESH_ORGANIZATION);
        if (defaultOrganization.isEmpty()) {
            throw new RuntimeException("Default organization is missing (Run init.sql or use liquibase)");
        }

        zoneService.findByOrganizationId(defaultOrganization.get().getId())
            .stream().parallel()
            .forEach(this::initForZone);
    }

    private void initForZone(ZoneDTO zone) {
        log.info("> Initializing clusters in zone: {}", zone.getName());
        if (!clients.containsKey(zone.getZoneId())) {
            clients.put(zone.getZoneId(), new HashMap<>());
        }

        clusterService.findByZoneId(zone.getId())
            .stream().parallel()
            .forEach(this::initCluster);
    }

    private void initCluster(ClusterDTO cluster) {
        log.info(">> Initializing cluster: {}", cluster.getName());
        if (StringUtils.isEmpty(cluster.getKubeConfig())) {
            log.warn("Cluster {} could not be initialized because kubeConfig was not present!", cluster.getName());
            return;
        }

        try {
            final ApiStub stub = new ApiStub(
                cluster,
                Config.fromConfig(new StringReader(cluster.getKubeConfig())),
                new PrometheusClient(restTemplate, cluster.getPrometheusUrl()),
                kubeRayClients(cluster),
                aimClients(cluster),
                environment
            );
            clients.get(cluster.getZone().getZoneId())
                .put(
                    cluster.getCid(),
                    stub
                );
            log.info(">> {} initialized successfully", cluster.getName());

            initInformers(stub, cluster.getZone().getZoneId(), cluster);
        } catch (IOException e) {
            log.warn("Cluster {} could not be initialized with error message {}!", cluster.getName(), e.getMessage());
        }
    }

    private void initInformers(ApiStub stub, String zoneId, ClusterDTO cluster) {
        SharedInformerFactory factory = new SharedInformerFactory(stub.getApiClient());

        // Node informer
        CoreV1Api.APIlistNodeRequest nodeRequest =
            stub.getCoreV1Api().listNode();
        SharedIndexInformer<V1Node> nodeInformer =
            factory.sharedIndexInformerFor(
                (CallGeneratorParams params) -> {
                    nodeRequest.watch(params.watch);
                    nodeRequest.timeoutSeconds(params.timeoutSeconds);
                    nodeRequest.resourceVersion(params.resourceVersion);
                    return nodeRequest.buildCall(null);
                },
                V1Node.class,
                V1NodeList.class);

        nodeInformer.addEventHandler(new NodeEventHandler(zoneId, cluster, this));

        factory.startAllRegisteredInformers();
        log.info(">> Informers for clusterId {} have been initialized", cluster.getCid());
    }

    @Async
    public CompletableFuture<TaskResult<Void>> createRole(String namespace, ClusterDTO cluster, String roleName, List<K8RoleRulesDTO> rules) {
        log.info("Creating Role {}", roleName);
        return new CreateRoleTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            roleName,
            rules
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> createRoleBinding(String namespace, ClusterDTO cluster, String roleBindingName, String subject, String roleRef) {
        log.info("Creating RoleBinding {}", roleBindingName);
        return new CreateRoleBindingTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            roleBindingName,
            subject
            , roleRef
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> createServiceAccount(String namespace, ClusterDTO cluster, String serviceAccountName) {
        log.info("Creating ServiceAccount {}", serviceAccountName);
        return new CreateServiceAccountTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            serviceAccountName
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> createNamespace(String namespace, ClusterDTO cluster) {
        log.info("Creating namespace {}", namespace);
        return new CreateNamespaceTask(
            getApi(cluster),
            this.taskConfig,
            namespace
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteNamespace(String namespace, ClusterDTO cluster) {
        log.info("Deleting namespace {}", namespace);
        return new DeleteNamespaceTask(
            getApi(cluster),
            this.taskConfig,
            namespace
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> createNetworkPolicy(String namespace, ClusterDTO cluster) {
        log.info("Creating network policy for namespace {}", namespace);
        return new NetworkPolicyTask(
            getApi(cluster),
            this.taskConfig,
            namespace
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<String>> deployApplication(String namespace,
                                                                   ApplicationDTO application,
                                                                   ClusterDTO cluster,
                                                                   List<String> userNodes) {
        log.info("Deploying application {} in namespace {}, on cluster {}",
            application.getName(), namespace, cluster.getName());
        return new DeploymentTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            cluster,
            application,
            userNodes
        ).call();
    }


    @Async
    public CompletableFuture<TaskResult<Void>> createPVC(String namespace, ClusterDTO cluster, VolumeDTO volume, String storageClassName) {
        log.info("Creating PVC for volume {}", volume.getName());
        return new PersistentVolumeClaimTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            volume,
            storageClassName
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> createStorageClass(String namespace, ClusterDTO cluster, VolumeDTO volume) {
        log.info("Creating StorageClass for volume {}", volume.getName());
        return new StorageClassTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            volume
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> createService(String namespace,
                                                             ClusterDTO cluster,
                                                             String applicationName,
                                                             PortDTO port,
                                                             Map<String, String> selectors) {
        log.info("Creating service for application {} and port {}", applicationName, port.getName());
        return new ServiceTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            applicationName,
            port,
            selectors
        ).call();
    }


    @Async
    public CompletableFuture<TaskResult<String>> createIngress(String namespace, ClusterDTO cluster,
                                                               String publicHostname, String applicationName,
                                                               PortDTO port, List<String> middlewares) {
        log.info("Creating ingress for application {} and port {}", applicationName, port.getName());
        return new IngressTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            publicHostname,
            applicationName,
            port,
            middlewares
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<String>> createIngressTCP(String namespace,
                                                                  ClusterDTO cluster,
                                                                  String resourceName,
                                                                  String serviceName,
                                                                  Integer port,
                                                                  String publicHostname,
                                                                  List<String> entryPoints,
                                                                  List<String> middlewares) {
        log.info("Creating ingress TCP for resource {}", resourceName);
        return new IngressTCPTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            resourceName,
            serviceName,
            port,
            publicHostname,
            entryPoints,
            middlewares
        ).call();
    }


    @Async
    public CompletableFuture<TaskResult<Void>> deleteTaskRun(String namespace,
                                                             ClusterDTO cluster,
                                                             TaskRunDTO task) {
        return new DeleteTaskRunTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            task
        ).call();
    }


    @Async
    public CompletableFuture<TaskResult<Void>> createIpAllowMiddleware(String namespace,
                                                                       ClusterDTO cluster,
                                                                       String applicationName,
                                                                       PortDTO port,
                                                                       List<FirewallEntryDTO> firewallEntries) {
        log.info("Creating IP Allow Middleware for application {} and port {}", applicationName, port.getName());
        return new IpAllowMiddlewareTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            applicationName,
            port,
            firewallEntries
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> createIpAllowMiddlewareTCP(String namespace,
                                                                          ClusterDTO cluster,
                                                                          String resourceName,
                                                                          Integer port,
                                                                          List<FirewallEntryDTO> firewallEntries) {
        log.info("Creating IP Allow Middleware TCP for resource {} and port {}", resourceName, port);
        return new IpAllowMiddlewareTCPTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            resourceName,
            port,
            firewallEntries
        ).call();
    }


    @Async
    public CompletableFuture<TaskResult<String>> createPostgreSQLCluster(String namespace,
                                                                         ClusterDTO cluster,
                                                                         DatabaseDTO database,
                                                                         List<String> userNodes) {
        log.info("Creating PostgreSQL cluster {}", database.getName());
        return new PostgreSQLTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            cluster,
            database,
            userNodes
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<InputStream>> readLogs(String namespace,
                                                               ClusterDTO cluster,
                                                               OutputStream outputStream,
                                                               Integer tailLines,
                                                               Integer sinceSeconds,
                                                               String specificPodName,
                                                               String specificContainerName) {
        return new ReadLogTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            outputStream,
            tailLines,
            sinceSeconds,
            specificPodName,
            specificContainerName
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Metrics>> readAimMetrics(ClusterDTO cluster,
                                                                 String rayCluster,
                                                                 String jobId) {
        return new AimMetricsTask(
            getApi(cluster),
            rayCluster,
            jobId
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Metrics>> readMetrics(String namespace,
                                                              ClusterDTO cluster,
                                                              MetricType metricType,
                                                              NodeDTO node,
                                                              ApplicationDTO application,
                                                              ContainerDTO container,
                                                              Integer gpuId,
                                                              String podName,
                                                              Long timeStart,
                                                              Long timeEnd,
                                                              Long step) {
        log.trace("Reading metrics");
        return new PrometheusMetricsTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            metricType,
            node,
            application,
            container,
            gpuId,
            podName,
            timeStart,
            timeEnd,
            step
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> cancelTaskRun(String namespace,
                                                             ClusterDTO cluster,
                                                             TaskRunDTO task) {
        log.trace("Cancel TaskRun in namespace {}, on cluster {}", namespace, cluster.getName());
        return new CancelTaskRunTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            task
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> cancelRayJob(String namespace,
                                                            ClusterDTO cluster,
                                                            RayJobDTO rayJob) {
        log.trace("Cancel RayJob in namespace {}, on cluster {}", namespace, cluster.getName());
        return new CancelRayJobTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            rayJob
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<String>> runTask(ClusterDTO cluster, TaskRunDTO taskRun, String namespace) {
        return new CreateTaskRunTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            taskRun,
            this.applicationProperties
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<List<ResourceStatus>>> readAppStatus(String namespace,
                                                                             ClusterDTO cluster,
                                                                             ApplicationDTO application,
                                                                             String podName,
                                                                             String component) {
        log.trace("Reading App status in namespace {}, on cluster {}", namespace, cluster.getName());
        return new AppStatusTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            application,
            podName,
            component
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> getAppShell(String namespace,
                                                           ApplicationDTO application,
                                                           String podName,
                                                           ContainerDTO container,
                                                           ClusterDTO cluster,
                                                           OutputStream outputStream,
                                                           InputStream inputStream,
                                                           Integer columns) {
        log.info("Opening shell for application {} in namespace {}, on cluster {}",
            application.getName(), namespace, cluster.getName());
        return new AppShellTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            application,
            podName,
            container,
            outputStream,
            inputStream,
            columns,
            smTaskExecutor
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> control(String namespace,
                                                       ClusterDTO cluster,
                                                       ControlTask.ControlObject controlObject,
                                                       ControlTask.ControlCommand controlCommand,
                                                       ApplicationDTO application,
                                                       NodeDTO node,
                                                       Integer replicas) {
        log.info("Controlling {} with command {}, on cluster {}", controlObject, controlCommand, cluster.getName());
        return new ControlTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            controlObject,
            controlCommand,
            application,
            node,
            replicas
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> copyFileToPod(String namespace,
                                                             ClusterDTO cluster,
                                                             ApplicationDTO application,
                                                             String podName,
                                                             ContainerDTO container,
                                                             String sourcePath,
                                                             String destinationPath) {
        log.info("Copying file to application {} in namespace {}, on cluster {}",
            application.getName(), namespace, cluster.getName());
        return new UploadFileTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            application,
            podName,
            container,
            sourcePath,
            destinationPath
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<String>> createRayJob(String namespace, ClusterDTO cluster, RayJobDTO rayJob, RayClusterShape rayClusterShape) {
        log.info("Creating ray job {}", rayJob.getName());
        return new RayJobTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            rayJob,
            rayClusterShape
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteRayJob(String namespace, ClusterDTO cluster, RayJobDTO rayJob) {
        log.info("Deleting ray job {}", rayJob.getName());
        return new DeleteRayJobTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            rayJob
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<List<TaskRunStatus>>> readTaskRunStatuses(String namespace,
                                                                                  ClusterDTO cluster,
                                                                                  Set<TaskRunDTO> tasks) {
        log.trace("Reading TaskRun status in namespace {}, on cluster {}", namespace, cluster.getName());
        return new ReadTaskRunStatusTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            tasks
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<List<RayJobStatus>>> readRayJobStatuses(String namespace,
                                                                                ClusterDTO cluster,
                                                                                Set<RayJobDTO> jobs) {
        log.trace("Reading RayJob status in namespace {}, on cluster {}", namespace, cluster.getName());
        return new ReadRayJobStatusTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            jobs
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<InputStream>> copyFileFromPod(String namespace,
                                                                      ClusterDTO cluster,
                                                                      ApplicationDTO application,
                                                                      String podName,
                                                                      ContainerDTO container,
                                                                      String sourcePath) {
        log.info("Copying file from application {} in namespace {}, on cluster {}",
            application.getName(), namespace, cluster.getName());
        return new DownloadFileTask(
            getApi(cluster),
            this.taskConfig,
            namespace,
            application,
            podName,
            container,
            sourcePath
        ).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteApplication(String namespace, ClusterDTO cluster, ApplicationDTO application) {
        log.info("Deleting application on cluster {}", cluster.getName());
        return new DeleteApplicationTask(getApi(cluster), this.taskConfig, namespace, application).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteDatabase(String namespace, ClusterDTO cluster, DatabaseDTO database) {
        log.info("Deleting database on cluster {}", cluster.getName());
        return new DeletePostgreSQLTask(getApi(cluster), this.taskConfig, namespace, database).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteService(String namespace, ClusterDTO cluster, String serviceName) {
        log.info("Deleting service on cluster {}", cluster.getName());
        return new DeleteServiceTask(getApi(cluster), this.taskConfig, namespace, serviceName).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteVolumeClaim(String namespace, ClusterDTO cluster, String pvcName) {
        log.info("Deleting PVC on cluster {}", cluster.getName());
        return new DeletePersistentVolumeClaimTask(getApi(cluster), this.taskConfig, namespace, pvcName).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteStorageClass(String namespace, ClusterDTO cluster, String scName) {
        log.info("Deleting StorageClass on cluster {}", cluster.getName());
        return new DeleteStorageClassTask(getApi(cluster), this.taskConfig, namespace, scName).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteIngress(String namespace, ClusterDTO cluster, ResourceDTO application, String portName) {
        log.info("Deleting ingress on cluster {}", cluster.getName());
        return new DeleteIngressTask(getApi(cluster), this.taskConfig, namespace, application, portName).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteIngressTCP(String namespace, ClusterDTO cluster, String resourceName, Integer port) {
        log.info("Deleting ingress TCP on cluster {}", cluster.getName());
        return new DeleteIngressTCPTask(getApi(cluster), this.taskConfig, namespace, resourceName, port).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteMiddleware(String namespace, ClusterDTO cluster, String middleware) {
        log.info("Deleting middleware on cluster {}", cluster.getName());
        return new DeleteMiddlewareTask(getApi(cluster), this.taskConfig, namespace, middleware).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteMiddlewareTCP(String namespace, ClusterDTO cluster, String middleware) {
        log.info("Deleting middleware TCP on cluster {}", cluster.getName());
        return new DeleteMiddlewareTCPTask(getApi(cluster), this.taskConfig, namespace, middleware).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> createSecret(String name, String type, Map<String, String> labels,
                                                            Map<String, byte[]> data, String namespace, ClusterDTO cluster) {
        log.info("Creating secret on cluster {}", cluster.getName());
        return new CreateSecretTask(name, type, labels, data, getApi(cluster), this.taskConfig, namespace).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteSecret(String name, String namespace, ClusterDTO cluster) {
        log.info("Deleting secret on cluster {}", cluster.getName());
        return new DeleteSecretTask(name, getApi(cluster), this.taskConfig, namespace).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> addNodeLabel(ClusterDTO cluster, NodeDTO node, String key, String value) {
        log.debug("Adding label on node {}", node.getInternalName());
        return new AddNodeLabelTask(getApi(cluster), this.taskConfig, null, node, key, value).call();
    }

    @Async
    public CompletableFuture<TaskResult<Void>> deleteNode(String name, ClusterDTO cluster) {
        log.info("Deleting node {}", name);
        return new DeleteNodeTask(getApi(cluster), this.taskConfig, null, name).call();
    }

    public V1NodeList listNodes(CoreV1Api coreV1Api) throws ApiException {
        return coreV1Api.listNode().execute();
    }

    public V1Node getNodeStatus(NodeDTO node) throws ApiException {
        return getApi(node.getCluster()).getCoreV1Api()
            .readNodeStatus(node.getInternalName()).execute();
    }

    public V1ObjectMeta getNodeMetadata(NodeDTO node) throws ApiException {
        return getApi(node.getCluster()).getCoreV1Api()
            .readNode(node.getInternalName()).execute()
            .getMetadata();
    }

    public boolean isNodeReady(NodeDTO node) throws ApiException {
        var kNode = getNodeStatus(node);
        if (kNode != null && kNode.getStatus() != null && kNode.getStatus().getConditions() != null) {
            return kNode.getStatus().getConditions().stream()
                .anyMatch(c -> "Ready".equals(c.getType()) && "True".equals(c.getStatus()));
        } else {
            log.warn("Node status not available for node {}", node.getInternalName());
            return false;
        }
    }

    public String selectClusterForApplication(String zoneId, ApplicationDTO application) {
        if (application == null) {
            return selectCluster(zoneId);
        }

        return ClusterSelectionFactory
            .selectForApplication(this, application)
            .select(clients.get(zoneId));
    }

    public String selectClusterForDatabase(String zoneId, DatabaseDTO database) {
        if (database == null) {
            return selectCluster(zoneId);
        }

        return ClusterSelectionFactory
            .selectForDatabase(this, database)
            .select(clients.get(zoneId));
    }

    protected String selectCluster(String zoneId) {
        // Default (Stochastic) selector
        return clusterSelector.select(clients.get(zoneId));
    }

    public String selectClusterForRayJob(String zoneId) {
        return ClusterSelectionFactory
            .selectForRayJob()
            .select(clients.get(zoneId));
    }

    public String selectClusterForTaskRun(String zoneId) {
        return ClusterSelectionFactory
            .selectForTaskRun()
            .select(clients.get(zoneId));
    }

    public ApiStub getApi(ClusterDTO cluster) {
        return clients.get(cluster.getZone().getZoneId()).get(cluster.getCid());
    }

    private Map<String, KubeRayClient> kubeRayClients(ClusterDTO cluster) {
        if (applicationProperties.getProfile().getRayClusters() == null) {
            return Collections.emptyMap();
        }
        return applicationProperties.getProfile().getRayClusters().stream()
            .filter(ray -> cluster.getCid().equals(ray.getCid()))
            .map(ray -> new KubeRayClient(restTemplate, ray))
            .collect(Collectors.toMap(KubeRayClient::name, client -> client));
    }

    private Map<String, AimClient> aimClients(ClusterDTO cluster) {
        if (applicationProperties.getProfile().getRayClusters() == null) {
            return Collections.emptyMap();
        }
        return applicationProperties.getProfile().getRayClusters().stream()
            .filter(ray -> cluster.getCid().equals(ray.getCid()))
            .map(ray -> new AimClient(restTemplate, ray, applicationProperties.getK8sAccessMode()))
            .collect(Collectors.toMap(AimClient::name, client -> client));
    }

    private void initConfig() {
        this.controllerConfig = new ControllerConfig(
            applicationProperties.getCheckNodeResourceUpdates()
        );
        this.taskConfig = new TaskConfig(
            applicationProperties.getK8sTask().getResourceOperationPollInterval(),
            applicationProperties.getK8sTask().getResourceOperationWaitTimeout(),
            applicationProperties.getK8sTask().getResourceOperationWatchTimeout(),
            applicationProperties.getK8sTask().getRequestVsLimitsCoefficientCpu(),
            applicationProperties.getK8sTask().getRequestVsLimitsCoefficientMemory(),
            applicationProperties.getJob().isTerminatingPodsDeleteFinalizers(),
            applicationProperties.getToken().getTtl()
        );
        this.storageConfig = new StorageConfig(
            applicationProperties.getStorage().getSmStorageBucketUrl(),
            applicationProperties.getStorage().getSmStorageAccessKey(),
            applicationProperties.getStorage().getSmStorageAccessSecret()
        );
    }
}
