package net.statemesh.k8s.task.db;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.options.CreateOptions;
import net.statemesh.domain.enumeration.Profile;
import net.statemesh.k8s.crd.postgresql.models.*;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.DatabaseDTO;
import net.statemesh.service.dto.VolumeDTO;
import net.statemesh.service.dto.VolumeMountDTO;
import net.statemesh.service.util.ProfileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

import static net.statemesh.k8s.util.ApiUtils.getStorageClassesByDatacenter;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.storageClassName;

public class PostgreSQLTask extends BaseMutationTask<String> {
    private final Logger log = LoggerFactory.getLogger(PostgreSQLTask.class);

    private static final String DEFAULT_TEAM = "acid";
    private static final String DEFAULT_STORAGE_ALLOCATION = "10Gi";

    private final ClusterDTO cluster;
    private final DatabaseDTO database;
    private final List<String> userNodes;

    public PostgreSQLTask(ApiStub apiStub,
                          TaskConfig taskConfig,
                          String namespace,
                          ClusterDTO cluster,
                          DatabaseDTO database,
                          List<String> userNodes) {
        super(apiStub, taskConfig, namespace);
        this.cluster = cluster;
        this.database = database;
        this.userNodes = userNodes;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<String> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create PostgreSQL cluster {} if not exists", database.getName());

        if (!dbClusterExists(getApiStub(), getNamespace(), database.getInternalName())) {
            log.debug("Create PostgreSQL cluster {}", database.getInternalName());
            getApiStub().getPostgreSQL().create(
                getNamespace(),
                new V1Postgresql()
                    .apiVersion(POSTGRESQL_GROUP + "/" + POSTGRESQL_API_VERSION)
                    .kind(POSTGRESQL_KIND)
                    .metadata(
                        new V1ObjectMeta()
                            .name(database.getInternalName())
                            .namespace(getNamespace())
                            .labels(labels())
                    )
                    .spec(
                        new V1PostgresqlSpec()
                            .teamId(DEFAULT_TEAM)
                            .numberOfInstances(database.getReplicas())
                            .postgresql(
                                new V1PostgresqlSpecPostgresql().version(V1PostgresqlSpecPostgresql.VersionEnum._17)
                            )
                            .volume(
                                database.getVolumeMounts().stream()
                                    .findAny()
                                    .map(VolumeMountDTO::getVolume)
                                    .map(this::toVolume)
                                    .orElse(defaultVolume())
                            ) // We don't support more than one volume for now
                            .patroni(new V1PostgresqlSpecPatroni().pgHba(POSTGRESQL_HBA_CONF))
                            .nodeAffinity(affinity())
                            .resources(
                                new V1PostgresqlSpecResources()
                                    .limits(
                                        new V1PostgresqlSpecResourcesLimits()
                                            .cpu(database.getCpuLimit().toString())
                                            .memory(database.memLimit())
                                    )
                                    .requests(
                                        new V1PostgresqlSpecResourcesLimits()
                                            .cpu(cpuRequest(database.getCpuLimit()).toString())
                                            .memory(memoryRequest(database.getMemLimit()).intValue() + "Mi")
                                    )
                            )
                    ),
                new CreateOptions()
            );
        } else {
            log.debug("Skipping PostgreSQL cluster {} creation as it exists", database.getInternalName());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## PostgreSQL cluster :: {} :: wait poll step", database.getInternalName());
        return dbClusterExists(getApiStub(), getNamespace(), database.getInternalName());
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<String> taskResult, boolean ready) {
        log.info("PostgreSQL cluster {} created successfully [{}]", database.getInternalName(), ready);
    }

    private Map<String, String> labels() {
        var labels = new HashMap<>(NamingUtils.appLabels(database.getInternalName()));
        labels.put(IGNORE_POLICY_LABEL, IGNORE_POLICY_LABEL_VALUE);
        return labels;
    }

    private V1PostgresqlSpecVolume toVolume(VolumeDTO volume) {
        List<String> datacenterStorageClasses = Collections.emptyList();
        try {
            datacenterStorageClasses = getStorageClassesByDatacenter(getApiStub(), database.getProject().getDatacenterName());
        } catch (ApiException e) {
            log.warn("Could not retrieve datacenter storageClass names");
        }

        final String storageClass;
        if (Profile.HPC.equals(database.getProject().getProfile()) && StringUtils.isEmpty(volume.getBucketUrl())) {
            storageClass = datacenterStorageClasses.isEmpty() ? LOCAL_PATH_STORAGE_CLASS : datacenterStorageClasses.getFirst();
        } else if (Profile.GPU.equals(database.getProject().getProfile())) {
            storageClass = LOCAL_PATH_STORAGE_CLASS;
        } else {
            storageClass = storageClassName(volume.getName(), !StringUtils.isEmpty(volume.getBucketUrl()));
        }

        return new V1PostgresqlSpecVolume()
            .storageClass(storageClass)
            .size(volume.getSize() + "Gi");
    }

    private V1PostgresqlSpecVolume defaultVolume() { // Default storageClass (local-path)
        return new V1PostgresqlSpecVolume().size(DEFAULT_STORAGE_ALLOCATION);
    }

    private V1PostgresqlSpecNodeAffinity affinity() {
        final var requiredAffinitySelector = new V1PostgresqlSpecNodeAffinityRequiredDuringSchedulingIgnoredDuringExecution();

        if (ProfileUtil.isCloud(getApiStub().getEnvironment())) {
            requiredAffinitySelector.addNodeSelectorTermsItem(
                new V1PostgresqlSpecNodeAffinityPreference()
                    .addMatchExpressionsItem(notOnMasterNodes())
            );
        }

        V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions deploymentProfile = deploymentProfile();
        if (deploymentProfile != null) {
            if (requiredAffinitySelector.getNodeSelectorTerms().isEmpty()) {
                requiredAffinitySelector.addNodeSelectorTermsItem(
                    new V1PostgresqlSpecNodeAffinityPreference()
                );
            }
            requiredAffinitySelector.getNodeSelectorTerms().getFirst().addMatchExpressionsItem(deploymentProfile);
        }

        return new V1PostgresqlSpecNodeAffinity()
            .requiredDuringSchedulingIgnoredDuringExecution(requiredAffinitySelector);
    }

    private V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions notOnMasterNodes() {
        return new V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions()
            .key(METADATA_NODE_IS_MASTER)
            .operator(OPERATOR_NOTIN)
            .addValuesItem(Boolean.TRUE.toString());
    }

    private V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions freeTierNodes(boolean dbIsFree) {
        return new V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions()
            .key(NODE_PRICE_LABEL)
            .operator(dbIsFree ? OPERATOR_IN : OPERATOR_NOTIN)
            .addValuesItem("0.0");
    }

    private V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions deploymentProfile() {
        if (database.getProject().getProfile() == null) {
            return null;
        }
        return switch (database.getProject().getProfile()) {
            case GPU -> gpuProfile();
            case HPC -> hpcProfile();
            case MYNODE -> userNodesProfile();
            case CLOUD -> cloudProfile();
            case EDGE -> edgeProfile();
            default -> null; // HYBRID and null profiles go without restrictions
        };
    }

    private V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions gpuProfile() {
        return new V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions()
            .key(HAS_GPU_LABEL)
            .operator(OPERATOR_EXISTS);
    }

    private V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions hpcProfile() {
        if (StringUtils.isEmpty(database.getProject().getDatacenterName())) {
            return null;
        }
        return new V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions()
            .key(DATACENTER_NAME_LABEL)
            .operator(OPERATOR_IN)
            .addValuesItem(database.getProject().getDatacenterName());
    }

    private V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions userNodesProfile() {
        return new V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions()
            .key(METADATA_SMID_KEY)
            .operator(OPERATOR_IN)
            .values(userNodes);
    }

    private V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions cloudProfile() {
        return new V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions()
            .key(EDGE_NODE_LABEL)
            .operator(OPERATOR_DOESNOTEXIST);
    }

    private V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions edgeProfile() {
        return new V1PostgresqlSpecNodeAffinityPreferenceMatchExpressions()
            .key(EDGE_NODE_LABEL)
            .operator(OPERATOR_EXISTS);
    }

    private boolean dbClusterExists(ApiStub apiStub, String namespace, String name) throws ApiException {
        V1PostgresqlList dbs = apiStub.getPostgreSQL().list(namespace).getObject();
        return dbs != null && dbs.getItems().stream()
            .anyMatch(db -> name.equals(Objects.requireNonNull(db.getMetadata()).getName()));
    }

    private BigDecimal cpuRequest(Double cpuLimit) {
        final double coefficient = cluster.getRequestVsLimitsCoefficientCpu() != null ?
            cluster.getRequestVsLimitsCoefficientCpu() :
            getTaskConfig().requestVsLimitsCoefficientCpu();
        return BigDecimal.valueOf(coefficient).multiply(
            BigDecimal.valueOf(cpuLimit)
        );
    }

    private BigDecimal memoryRequest(String memoryLimit) {
        final double coefficient = cluster.getRequestVsLimitsCoefficientMemory() != null ?
            cluster.getRequestVsLimitsCoefficientMemory() :
            getTaskConfig().requestVsLimitsCoefficientMemory();
        return BigDecimal.valueOf(coefficient).multiply(
            Quantity.fromString(memoryLimit).getNumber()
        );
    }
}
