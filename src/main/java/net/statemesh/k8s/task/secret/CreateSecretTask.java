package net.statemesh.k8s.task.secret;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

public class CreateSecretTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(CreateSecretTask.class);

    private final String name;
    private final String type;
    private final Map<String, String> labels;
    private final Map<String, byte[]> data;

    public CreateSecretTask(
        String name,
        String type,
        Map<String, String> labels,
        Map<String, byte[]> data,
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace
    ) {
        super(apiStub, taskConfig, namespace);
        this.name = name;
        this.type = type;
        this.labels = labels;
        this.data = data;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws Exception {
        log.info("Creating secret {} of type {} if not exists", name, type);

        if (!secretExists(name)) {
            log.debug("Create secret {}", name);
            getApiStub().getCoreV1Api()
                .createNamespacedSecret(
                    getNamespace(),
                    new V1Secret()
                        .metadata(
                            new V1ObjectMeta()
                                .namespace(getNamespace())
                                .name(name)
                                .labels(labels != null ? labels : Collections.emptyMap())
                        )
                        .type(type)
                        .immutable(Boolean.TRUE)
                        .data(data)
                )
                .execute();
        } else {
            log.debug("Skipping secret {} creation as it exists", name);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Secret :: {} :: wait poll step", name);
        return secretExists(name);
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Secret created successfully [{}]", ready);
    }

    protected boolean secretExists(String name) throws ApiException {
        try {
            V1Secret secret = getApiStub().getCoreV1Api().readNamespacedSecret(name, getNamespace()).execute();
            return secret.getData() != null;
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return Boolean.FALSE;
            }
            throw e;
        }
    }
}
