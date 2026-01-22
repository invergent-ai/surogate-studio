package net.statemesh.k8s.util;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.openapi.models.V1NodeStatus;
import net.statemesh.service.dto.*;

import java.math.BigDecimal;
import java.util.Objects;

import static net.statemesh.k8s.util.K8SConstants.CPU_METRIC_KEY;
import static net.statemesh.k8s.util.K8SConstants.MEMORY_METRIC_KEY;
import static org.apache.commons.lang3.compare.ComparableUtils.is;

public class ResourceUtil {
    private static final Long GB_1 = 1073741824L;
    private static final BigDecimal HIGH_CPU_THRESHOLD = new BigDecimal(16);
    private static final BigDecimal LOW_CPU_THRESHOLD = new BigDecimal(4);
    private static final BigDecimal HIGH_MEMORY_THRESHOLD = new BigDecimal(16*GB_1);
    private static final BigDecimal LOW_MEMORY_THRESHOLD = new BigDecimal(4*GB_1);

    public static Boolean highAppCPURequirement(ApplicationDTO application) {
        if (application == null || application.getContainers() == null) {
            return Boolean.FALSE;
        }

        return application.getContainers().stream()
            .map(ContainerDTO::getCpuLimit)
            .filter(Objects::nonNull)
            .reduce(0d, Double::sum) >= HIGH_CPU_THRESHOLD.intValue();
    }

    public static Boolean highAppMemoryRequirement(ApplicationDTO application) {
        if (application == null || application.getContainers() == null) {
            return Boolean.FALSE;
        }

        return is(application.getContainers().stream()
            .map(ContainerDTO::getMemLimit)
            .filter(Objects::nonNull)
            .map(Quantity::fromString)
            .filter(Objects::nonNull)
            .map(Quantity::getNumber)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add)).greaterThanOrEqualTo(HIGH_MEMORY_THRESHOLD);
    }

    public static Boolean highDatabaseCPURequirement(DatabaseDTO database) {
        if (database == null) {
            return Boolean.FALSE;
        }
        return database.getCpuLimit() >= HIGH_CPU_THRESHOLD.intValue();
    }

    public static Boolean highDatabaseMemoryRequirement(DatabaseDTO database) {
        if (database == null) {
            return Boolean.FALSE;
        }
        return is(Quantity.fromString(database.getMemLimit()).getNumber())
            .greaterThanOrEqualTo(HIGH_MEMORY_THRESHOLD);
    }

    public static Boolean lowCPUAvailable(V1NodeList nodes) {
        return lowResourceAvailable(nodes, CPU_METRIC_KEY, LOW_CPU_THRESHOLD);
    }

    public static Boolean lowMemoryAvailable(V1NodeList nodes) {
        return lowResourceAvailable(nodes, MEMORY_METRIC_KEY, LOW_MEMORY_THRESHOLD);
    }

    private static Boolean lowResourceAvailable(V1NodeList nodes, String resource, BigDecimal threshold) {
        if (nodes == null || nodes.getItems().isEmpty()) {
            return Boolean.FALSE;
        }

        return is(computeResource(nodes, resource)).lessThan(threshold);
    }

    public static BigDecimal computeResource(V1NodeList nodes, String resource) {
        return nodes.getItems().stream()
            .map(V1Node::getStatus)
            .filter(Objects::nonNull)
            .map(V1NodeStatus::getAllocatable)
            .filter(Objects::nonNull)
            .filter(map -> map.containsKey(resource))
            .map(map -> map.get(resource))
            .filter(Objects::nonNull)
            .map(Quantity::getNumber)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static Boolean highCPUResources(NodeBenchmarkDTO nodeBenchmark) {
        return is(nodeBenchmark.getCpuLogicalCores()).greaterThanOrEqualTo(HIGH_CPU_THRESHOLD.intValue());
    }
}
