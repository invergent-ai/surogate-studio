package net.statemesh.k8s.task.node;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Node;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.NodeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import static io.kubernetes.client.extended.kubectl.Kubectl.label;

public class DeleteNodeLabelTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteNodeLabelTask.class);

    private final NodeDTO node;
    private final String key;

    public DeleteNodeLabelTask(ApiStub apiStub,
                            TaskConfig taskConfig,
                            String namespace,
                            NodeDTO node,
                            String key) {
        super(apiStub, taskConfig, namespace);
        this.node = node;
        this.key = key;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        log.debug("Deleting labels on node {}", node.getInternalName());

        if (labelExists()) {
            label(V1Node.class)
                .apiClient(getApiStub().getApiClient())
                .name(node.getInternalName())
                .addLabel(key, null)
                .execute();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Delete label on node with name {} :: wait poll step", node.getInternalName());
        return !labelExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.debug("Delete label to node {} finished successfully [{}]", node.getInternalName(), ready);
    }

    private boolean labelExists() throws ApiException {
        V1Node vnode = getApiStub().getCoreV1Api().readNode(node.getInternalName()).execute();
        return Objects.requireNonNull(Objects.requireNonNull(vnode.getMetadata()).getLabels()).containsKey(key);
    }
}
