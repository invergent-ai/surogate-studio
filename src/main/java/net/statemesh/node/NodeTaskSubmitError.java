package net.statemesh.node;

import lombok.Getter;

@Getter
public class NodeTaskSubmitError extends RuntimeException {
    private final String nodeIp;
    private final String taskId;

    public NodeTaskSubmitError(String nodeIp, String taskId) {
        super(String.format("Task execution error on node %s: %s", nodeIp, taskId));
        this.nodeIp = nodeIp;
        this.taskId = taskId;
    }
}
