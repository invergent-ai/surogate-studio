package net.statemesh.domain.enumeration;

/**
 * Node status ENUM
 */
public enum NodeStatus {
    // the node was just installed and system pods are being deployed
    PENDING,
    // system pods were deployed, the node is ready to accept pods
    READY,
    // the node's kubelet is not reachable (eg. network issues, machine down) and the node needs restart
    NOT_REACHABLE,
    // the node is being weird and needs to be restarted
    KUBELET_NOT_HEALTHY
}
