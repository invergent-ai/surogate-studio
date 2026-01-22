package net.statemesh.node;

import lombok.Getter;

@Getter
public class NodeTaskTimeoutError extends RuntimeException {
    private final String nodeIp;
    private final String taskId;

    public NodeTaskTimeoutError(String nodeIp, String taskId) {
        super(String.format("Task timeout on node %s: %s", nodeIp, taskId));
        this.nodeIp = nodeIp;
        this.taskId = taskId;
    }
}
