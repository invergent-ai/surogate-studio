package net.statemesh.k8s.task.node;

import io.kubernetes.client.common.KubernetesListObject;
import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteNodeTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteNodeTask.class);

    private final String name;

    public DeleteNodeTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String name
    ) {
        super(apiStub, taskConfig, namespace);
        this.name = name;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        log.info("Deleting node {} if exists", name);
        if (nodeExists()) {
            log.debug("Delete node {}", name);
            getApiStub().getCoreV1Api().deleteNode(name).execute();
        } else {
            log.debug("Skipping node {} deletion as it does not exist", name);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Node delete :: {} :: wait poll step", name);
        return !nodeExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Node deleted successfully [{}]", ready);
    }

    private boolean nodeExists() throws ApiException {
        KubernetesListObject nodes = getApiStub().getCoreV1Api().listNode().execute();
        return nodes.getItems().stream()
            .anyMatch(node -> name.equals(node.getMetadata().getName()));
    }
}
