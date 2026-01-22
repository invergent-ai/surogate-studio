package net.statemesh.k8s.task;

import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.ApiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

public abstract class BaseTask<T> implements Callable<CompletableFuture<TaskResult<T>>> {
    private final Logger log = LoggerFactory.getLogger(BaseTask.class);

    private final ApiStub apiStub;
    private final TaskConfig taskConfig;
    private final String namespace;

    public BaseTask(ApiStub apiStub,
                    TaskConfig taskConfig,
                    String namespace) {
        this.apiStub = apiStub;
        this.taskConfig = taskConfig;
        this.namespace = namespace;
    }

    protected ApiStub getApiStub() {
        return apiStub;
    }

    protected TaskConfig getTaskConfig() {
        return taskConfig;
    }

    protected String getNamespace() {
        return namespace;
    }

    protected void deleteDanglingPods() {
        log.trace("Deleting dangling pods");
        ApiUtils.deleteDanglingPods(getApiStub(), getNamespace(), taskConfig.deleteFinalizers());
    }

    @Override
    public abstract CompletableFuture<TaskResult<T>> call() throws Exception;
}
