package net.statemesh.domain.enumeration;

import io.kubernetes.client.common.KubernetesObject;
import io.kubernetes.client.openapi.models.V1DaemonSet;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1StatefulSet;
import lombok.Getter;

/**
 * k8s workload type ENUM
 */
@Getter
public enum WorkloadType {
    DEPLOYMENT("Deployment", V1Deployment.class),
    STATEFUL_SET("StatefulSet", V1StatefulSet.class),
    DAEMON_SET("DaemonSet", V1DaemonSet.class);

    private final String value;
    private final Class<? extends KubernetesObject> clazz;

    WorkloadType(String value, Class<? extends KubernetesObject> clazz) {
        this.value = value;
        this.clazz = clazz;
    }
}
