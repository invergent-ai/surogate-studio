package net.statemesh.k8s.task.node;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;

public class DeleteHelmReleaseTask extends BaseMutationTask<Void> {
    private final String chartName;

    public DeleteHelmReleaseTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String chartName
    ) {
        super(apiStub, taskConfig, namespace);
        this.chartName = chartName;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        getApiStub().getHelmController().delete(getNamespace(), chartName);
    }

    @Override
    protected boolean isReady() throws ApiException {
        return true;
    }
}
