package net.statemesh.k8s.util;

import io.kubernetes.client.openapi.models.V1PodDNSConfigOption;

import java.util.List;

public class K8SConstants {
    public static final String API_VERSION = "v1";
    public static final String APPS_API_VERSION = "apps/v1";

    public static final String SYSTEM_NAMESPACE = "kube-system";
    public static final String DEFAULT_NAMESPACE = "default";

    public static final String METADATA_NODE_IS_MASTER = "node-role.kubernetes.io/master";
    public static final String METADATA_NODE_EXTERNAL_IP = "k3s.io/external-ip";
    public static final String METADATA_RESTART_KEY = "kubectl.kubernetes.io/restartedAt";
    public static final String METADATA_SMID_KEY = "smid";
    public static final String METADATA_IGNORE_EVENTS_KEY = "ignore-events";

    public static final String CPU_METRIC_KEY = "cpu";
    public static final String MEMORY_METRIC_KEY = "memory";
    public static final String GPU_METRIC_KEY = "nvidia.com/gpu";
    public static final String EPHEMERAL_STORAGE_METRIC_KEY = "ephemeral-storage";

    public static final String MEMORY_PRESSURE_CONDITION = "MemoryPressure";
    public static final String DISK_PRESSURE_CONDITION = "DiskPressure";
    public static final String PID_PRESSURE_CONDITION = "PIDPressure";
    public static final String KUBELET_READY_CONDITION = "Ready";

    public static final String INTERNAL_IP_ADDRESS = "InternalIP";
    public static final String HOSTNAME_ADDRESS = "Hostname";
    public static final String IPv4 = "IPv4";
    public static final String IPv6 = "IPv6";
    public static final String DEFAULT_SERVICE_PROTOCOL = "TCP";

    public static final String SHELL_CONSOLE_SH = "sh";
    public static final String SHELL_CONSOLE_INIT = "export COLUMNS=%s; /bin/bash \nclear\n";

    private static final String BOOTSTRAP_PROGRAM = "k3s";
    public static final String BOOTSTRAP_TOKEN_USAGE_SIGNING = "signing";
    public static final String BOOTSTRAP_TOKEN_USAGE_AUTH = "authentication";
    public static final String[] BOOTSTRAP_TOKEN_GROUPS =
        {"system:bootstrappers:" + BOOTSTRAP_PROGRAM + ":default-node-token"};
    public static final int BOOTSTRAP_TOKEN_ID_BYTES = 6;
    public static final int BOOTSTRAP_TOKEN_SECRET_BYTES = 16;
    public static final String BOOTSTRAP_TOKEN_SECRET_PREFIX = "bootstrap-token-";
    public static final String BOOTSTRAP_TOKEN_SECRET_TYPE = "bootstrap.kubernetes.io/token";
    public static final String BOOTSTRAP_TOKEN_ID_KEY = "token-id";
    public static final String BOOTSTRAP_TOKEN_SECRET_KEY = "token-secret";
    public static final String BOOTSTRAP_TOKEN_EXPIRATION_KEY = "expiration";
    public static final String BOOTSTRAP_TOKEN_EXTRA_GROUPS_KEY = "auth-extra-groups";
    public static final String BOOTSTRAP_TOKEN_USAGE_PREFIX = "usage-bootstrap-";
    public static final String BOOTSTRAP_TOKEN_PREFIX = "K10";

    public static final String SECRET_TYPE_OPAQUE = "Opaque";
    public static final String SECRET_TYPE_DOCKERHUB = "kubernetes.io/dockerconfigjson";
    public static final String SECRET_DOCKER_DATA_KEY = ".dockerconfigjson";

    public static final String PK_SECRET = "txdata";
    public static final String NODEAPP_RELEASE_PREFIX = "nodeapp-";

    public static final String RFC3339 = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String DEFAULT_SERVICE_TYPE = "ClusterIP";

    public static final String NAMESPACE_SELECTOR_LABEL_NAME = "kubernetes.io/metadata.name";
    public static final String SERVICE_SELECTOR_LABEL_NAME = "app.kubernetes.io/name";
    public static final String SERVICE_SELECTOR_LABEL_INSTANCE = "app.kubernetes.io/instance";
    public static final String SERVICE_SELECTOR_TAG = "sm-tag";
    public static final String SERVICE_SELECTOR_TAG_VALUE = "StateMesh";
    public static final String SERVICE_SELECTOR_STORAGE_SECRET_TAG = "sm-storage-tag";
    public static final String SERVICE_SELECTOR_STORAGE_SECRET_TAG_VALUE = "StateMeshStorage";
    public static final String SERVICE_SELECTOR_LABEL_CLUSTER_NAME = "cluster-name";
    public static final String SERVICE_SELECTOR_LABEL_RAY_CLUSTER = "ray.io/cluster";
    public static final String SERVICE_SELECTOR_LABEL_RAY_HEAD_SELECT = "head-select";
    public static final String SERVICE_SELECTOR_LABEL_RAY_CLUSTER_TYPE = "ray.io/node-type";
    public static final String SERVICE_SELECTOR_LABEL_KUBEVIRT_DOMAIN = "kubevirt.io/domain";
    public static final String SERVICE_SELECTOR_TASK_RUN = "tekton.dev/taskRun";
    public static final String TASK_RUN_STATUS_LABEL = "sky/state";

    public static final String DEFAULT_GROUP = "";
    public static final String PODS_PLURAL = "pods";
    public static final String STORAGE_CLASS_GROUP = "storage.k8s.io";
    public static final String STORAGE_CLASS_PLURAL = "storageclasses";

    public static final String HELM_CHART_GROUP = "helm.cattle.io";
    public static final String HELM_CHART_API_VERSION = "v1";
    public static final String HELM_CHART_PLURAL = "helmcharts";

    public static final String TRAEFIK_GROUP = "traefik.io";
    public static final String TRAEFIK_API_VERSION = "v1alpha1";
    public static final String TRAEFIK_INGRESS_ROUTE_PLURAL = "ingressroutes";
    public static final String TRAEFIK_INGRESS_ROUTE_KIND = "IngressRoute";
    public static final String TRAEFIK_INGRESS_TCP_PLURAL = "ingressroutetcps";
    public static final String TRAEFIK_INGRESS_TCP_KIND = "IngressRouteTCP";
    public static final String TRAEFIK_MIDDLEWARE_PLURAL = "middlewares";
    public static final String TRAEFIK_MIDDLEWARE_KIND = "Middleware";
    public static final String TRAEFIK_MIDDLEWARE_TCP_PLURAL = "middlewaretcps";
    public static final String TRAEFIK_MIDDLEWARE_TCP_KIND = "MiddlewareTCP";
    public static final String TRAEFIK_ACME_CERTRESOLVER = "smacme";
    public static final List<String> TRAEFIK_DEFAULT_ENTRYPOINTS = List.of("web", "websecure");

    public static final String TEKTON_GROUP = "tekton.dev";
    public static final String TEKTON_API_VERSION = "v1";
    public static final String TEKTON_PIPELINE_RUN_KIND = "PipelineRun";
    public static final String TEKTON_TASK_RUN_KIND = "TaskRun";
    public static final String TEKTON_PIPELINE_RUN_PLURAL = "pipelineruns";
    public static final String TEKTON_TASK_RUN_PLURAL = "taskruns";
    public static final List<String> TRAEFIK_DB_ENTRYPOINT = List.of("ssh");
    public static final String PUBLIC_DB_INGRESS_HOSTNAME_PREFIX = "db-";

    public static final String RAY_GROUP = "ray.io";
    public static final String RAY_CLUSTER_API_VERSION_V1 = "v1";
    public static final String RAY_CLUSTER_PLURAL = "rayclusters";
    public static final String RAY_SERVICE_API_VERSION_V1 = "v1";
    public static final String RAY_SERVICE_PLURAL = "rayservices";
    public static final String RAY_JOB_API_VERSION_V1 = "v1";
    public static final String RAY_JOB_PLURAL = "rayjobs";
    public static final String RAY_JOB_RUN_KIND = "RayJob";
    public static final boolean EXTERNAL_RAY_CLUSTER = false;

    public static final String VOLUME_SIZE_UNIT = "Gi";
    public static final List<String> PVC_ACCESS_MODES = List.of("ReadWriteOnce");
    public static final String PVC_VOLUME_MODE = "Filesystem";
    public static final String PVC_RESOURCE_KEY = "storage";

    public static final String STORAGE_CLASS_PROVISIONER = "csi.juicefs.com";
    public static final String STORAGE_CLASS_RECLAIM_POLICY = "Delete";
    public static final String STORAGE_CLASS_VOLUME_BINDING_MODE = "Immediate";
    public static final String STORAGE_CLASS_NODE_PUBLISH_SECRET_KEY = "csi.storage.k8s.io/node-publish-secret-name";
    public static final String STORAGE_CLASS_NODE_PUBLISH_NAMESPACE_KEY = "csi.storage.k8s.io/node-publish-secret-namespace";
    public static final String STORAGE_CLASS_PROVISIONER_SECRET_KEY = "csi.storage.k8s.io/provisioner-secret-name";
    public static final String STORAGE_CLASS_PROVISIONER_NAMESPACE_KEY = "csi.storage.k8s.io/provisioner-secret-namespace";

    public static final String STORAGE_SECRET_ACCESS_KEY_KEY = "access-key";
    public static final String STORAGE_SECRET_BUCKET_KEY = "bucket";
    public static final String STORAGE_SECRET_FORMAT_OPTIONS_KEY = "format-options";
    public static final String STORAGE_SECRET_META_URL_KEY = "metaurl";
    public static final String STORAGE_SECRET_FOLDER_NAME_KEY = "name";
    public static final String STORAGE_SECRET_SECRET_KEY_KEY = "secret-key";
    public static final String STORAGE_SECRET_STORAGE_KEY = "storage";

    public static final String STORAGE_SECRET_FORMAT_OPTIONS = "trash-days=1,block-size=4096";
    public static final String STORAGE_SECRET_STORAGE = "s3";

    public static final String DELETE_FINALIZERS_PATCH = "{\"metadata\":{\"finalizers\":null}}";

    public static final String PUBLIC_INGRESS_HOSTNAME_PREFIX = "app-";
    public static final String PUBLIC_VLLM_INGRESS_HOSTNAME_PREFIX = "vllm-";

    public static final int START_REDIS_INDEX = 2;
    public static final int ERROR_REDIS_INDEX = 1;

    public static final String OPERATOR_IN = "In";
    public static final String OPERATOR_NOTIN = "NotIn";
    public static final String OPERATOR_EXISTS = "Exists";
    public static final String OPERATOR_DOESNOTEXIST = "DoesNotExist";

    public static final String NODE_PRICE_LABEL = "nodePrice";
    public static final String DEFAULT_ENV_VAR_INGRESS_DOMAIN = "INGRESS_DOMAIN";
    public static final String DEFAULT_ENV_VAR_NAMESPACE = "NAMESPACE";
    public static final String DEFAULT_ENV_VAR_CLUSTER_ID = "CLUSTER_ID";
    public static final String DEFAULT_ENV_VAR_SERVICE_FQDN = "SERVICE_FQDN";

    public static final String HAS_GPU_LABEL = "hasGPU";
    public static final String EDGE_NODE_LABEL = "edgeNode";
    public static final String DATACENTER_NAME_LABEL = "datacenterName";
    public static final String RAY_CLUSTER_NAME_LABEL = "rayCluster";
    public static final String RAY_JOB_NAME_LABEL = "job-name";

    public static final String LOCAL_PATH_STORAGE_CLASS = "local-path";

    public static final V1PodDNSConfigOption DNS_HACK_FIX = new V1PodDNSConfigOption().name("ndots").value("1");

    public static final String DEFAULT_NS_NETWORK_POLICY_NAME = "isolate-namespace-egress";
    public static final String IGNORE_POLICY_LABEL = "ignore-policy";
    public static final String IGNORE_POLICY_LABEL_VALUE = "ignore";

    public static final String EXTERNAL_RAY_CLUSTER_DEFAULT_NAMESPACE = "default";
    public static final String RAY_CLUSTER_SELECTOR_LABEL = "ray.io/cluster";
    public static final String RAY_JOB_SUBMITTER_CONTAINER_NAME = "rayjob-submitter";
    public static final String RAY_NFS_STORAGE_CLASS = "nfs-csi-densemax-v2";
    public static final String RAY_WORKDIR_VOLUME_NAME = "ray-work-dir";
    public static final String RAY_WORK_DIR = "/opt/work";
    public static final String AIM_DIR = "/opt/aim";
    public static final String RAY_VERSION = "2.50.1";

    public static final String DENSEMAX_IMAGE = "ghcr.io/invergent-ai/densemax:1.0.0";
    public static final String SUROGATE_TRAIN_AXOLOTL_IMAGE = "ghcr.io/invergent-ai/train-axolotl:latest";
    public static final String SUROGATE_TRAIN_SUROGATE_IMAGE = "ghcr.io/invergent-ai/train-surogate:latest";

    public static final String TEKTON_JOB_NAME = "run-job";

    public static final String POSTGRESQL_GROUP = "acid.zalan.do";
    public static final String POSTGRESQL_API_VERSION = "v1";
    public static final String POSTGRESQL_KIND = "postgresql";
    public static final String POSTGRESQL_PLURAL = "postgresqls";

    // Zalando PostgreSQL Operator (Spilo) labels
    public static final String POSTGRESQL_SPILO_APPLICATION_LABEL = "application";
    public static final String POSTGRESQL_SPILO_APPLICATION_VALUE = "spilo";
    public static final String POSTGRESQL_SPILO_CLUSTER_LABEL = "cluster-name";

    // CloudNativePG labels
    public static final String CNPG_CLUSTER_LABEL = "cnpg.io/cluster";

    public static final List<String> NETWORK_POLICY_ALLOWED_NAMESPACES =
        List.of(SYSTEM_NAMESPACE, DEFAULT_NAMESPACE);

    public static final String DB_SECRET_PASSWORD_KEY = "password";

    public static final List<String> POSTGRESQL_HBA_CONF =
        List.of(
            "local     all             all                             trust",
            "local     replication     standby                         trust",
            "hostssl   all             +zalandos    127.0.0.1/32       pam",
            "host      all             all          127.0.0.1/32       md5",
            "hostssl   all             +zalandos    ::1/128            pam",
            "host      all             all          ::1/128            md5",
            "hostssl   replication     standby      all                md5",
            "hostnossl all             all          all                md5",
            "hostssl   all             +zalandos    all                pam",
            "hostssl   all             all          all                md5",
            "host      all             all          all                md5"
        );

    public static final String NFS_SERVER = "192.168.1.156";
    public static final String NFS_AIM_PATH = "/data/aim";
    public static final String NFS_AIM_PATH_TMP = "/data/aim-tmp";

    public static final String SKY_TASK_TIMEOUT = "720h";
    public static final String SKY_MANAGED_SERVICE_LABEL = "skypilot-cluster";

    public static final String NAD_SELECTOR_ANNOTATION = "k8s.v1.cni.cncf.io/networks";
    public static final String SRIOV_NAD_NAME = "default/sriov-rdma-network";
    public static final String SRIOV_RESOURCE_NAME = "nvidia.com/sriov_resource";
    public static final String GPU_RESOURCE_NAME = "nvidia.com/gpu";
    public static final String WAIT_FOR_GPU_IMAGE = "nvidia/cuda:12.3.1-base-ubuntu22.04";
    public static final String WAIT_FOR_GPU_SCRIPT =
        """
            for i in $(seq 1 60); do
               if nvidia-smi >/dev/null 2>&1; then exit 0; fi
               echo "GPU not ready yet... ($i/60)"; sleep 2
            done
            echo "GPU never became ready"; exit 1""";
}


