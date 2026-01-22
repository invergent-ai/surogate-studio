package net.statemesh.k8s.strategy;

import io.kubernetes.client.openapi.ApiException;
import lombok.Builder;
import net.statemesh.domain.enumeration.Profile;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.ResourceUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.statemesh.k8s.util.K8SConstants.CPU_METRIC_KEY;
import static net.statemesh.k8s.util.K8SConstants.MEMORY_METRIC_KEY;

public class ResourceClusterSelectionStrategy implements ClusterSelectionStrategy {
    private final Logger log = LoggerFactory.getLogger(ResourceClusterSelectionStrategy.class);

    private final KubernetesController kubernetesController;
    private final Resource resource;
    private final Type type;
    private final ProfileInfo profile;

    public ResourceClusterSelectionStrategy(KubernetesController kubernetesController,
                                            Resource resource,
                                            Type type,
                                            ProfileInfo profile) {
        this.kubernetesController = kubernetesController;
        this.resource = resource;
        this.type = type;
        this.profile = profile;
    }

    @Override
    public String select(Map<String, ApiStub> clusters) {
        return selectType(filter(clusters))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    private Map<String, ApiStub> filter(Map<String, ApiStub> clusters) {
        if (profile == null) {
            return clusters;
        }
        return switch (profile.profile()) {
            case GPU -> withGPU(clusters);
            case HPC -> withHPC(clusters);
            case MYNODE -> withUserNodes(clusters);
            default -> clusters;
        };
    }

    private Map<String, ApiStub> withGPU(Map<String, ApiStub> clusters) {
        return clusters.entrySet().stream()
            .filter(entry -> clusterHasGPU(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, ApiStub> withHPC(Map<String, ApiStub> clusters) {
        return clusters.entrySet().stream()
            .filter(entry -> clusterHasHPC(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, ApiStub> withUserNodes(Map<String, ApiStub> clusters) {
        return clusters.entrySet().stream()
            .filter(entry -> clusterHasUserNodes(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean clusterHasGPU(String clusterId) {
        return kubernetesController.getNodeService().nodeWithGPUExists(
            kubernetesController.getNodeService().findByClusterCid(clusterId)
        );
    }

    private boolean clusterHasHPC(String clusterId) {
        return StringUtils.isEmpty(profile.datacenter()) ?
            kubernetesController.getNodeService().nodeWithHPCExists(
                kubernetesController.getNodeService().findByClusterCid(clusterId)
            ) :
            kubernetesController.getNodeService().nodeWithDatacenterExists(
                kubernetesController.getNodeService().findByClusterCid(clusterId),
                profile.datacenter()
            );
    }

    private boolean clusterHasUserNodes(String clusterId) {
        return !kubernetesController.getNodeService().findByClusterCidAndUserId(clusterId, profile.userId()).isEmpty();
    }

    private Optional<AbstractMap.SimpleEntry<String, BigDecimal>> selectType(Map<String, ApiStub> clusters) {
        return Type.LOW.equals(type) ?
            gatherResources(clusters).min(comparator()) :
            gatherResources(clusters).max(comparator());
    }

    private Stream<AbstractMap.SimpleEntry<String, BigDecimal>> gatherResources(Map<String, ApiStub> clusters) {
        return clusters.entrySet().stream()
            .map(e -> new AbstractMap.SimpleEntry<>(
                e.getKey(),
                getResources(e.getValue(), resource.key())
            ));
    }

    private Comparator<Map.Entry<String, BigDecimal>> comparator() {
        return Map.Entry.comparingByValue();
    }

    private BigDecimal getResources(ApiStub stub, String resource) {
        try {
            return ResourceUtil.computeResource(
                kubernetesController.listNodes(stub.getCoreV1Api()),
                resource
            );
        } catch (ApiException e) {
            log.error("Could not get resources for nodes with message {}", e.getMessage());
        }

        return BigDecimal.ZERO;
    }

    @Builder
    public record ProfileInfo(Profile profile, String datacenter, String userId) {}

    public enum Resource {
        CPU(CPU_METRIC_KEY),
        MEMORY(MEMORY_METRIC_KEY);

        private final String key;

        Resource(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    public enum Type {
        HIGH,
        LOW
    }
}
