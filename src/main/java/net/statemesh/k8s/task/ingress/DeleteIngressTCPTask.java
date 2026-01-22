package net.statemesh.k8s.task.ingress;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.crd.traefik.models.V1alpha1IngressRouteTCPList;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.statemesh.k8s.util.NamingUtils.ingressName;

public class DeleteIngressTCPTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteIngressTCPTask.class);

    private final String resourceName;
    private final Integer port;

    public DeleteIngressTCPTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String resourceName,
        Integer port
    ) {
        super(apiStub, taskConfig, namespace);
        this.resourceName = resourceName;
        this.port = port;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Deleting ingress TCP for resource {} if exists", resourceName);
        if (ingressTCPExists()) {
            log.debug("Delete ingress TCP for resource {}", resourceName);
            getApiStub().getTraefikTCPIngress().delete(getNamespace(), ingressName(resourceName, port.toString()));
        } else {
            log.debug("Skipping ingress TCP deletion for resource {} as it does not exist", resourceName);
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Ingress TCP delete :: {} :: wait poll step", resourceName);
        return !ingressTCPExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Ingress TCP deleted successfully [{}]", ready);
    }

    private boolean ingressTCPExists() {
        V1alpha1IngressRouteTCPList routes = getApiStub().getTraefikTCPIngress().list(getNamespace()).getObject();
        return routes != null && routes.getItems().stream()
            .anyMatch(ingress -> ingressName(resourceName, port.toString()).equals(ingress.getMetadata().getName()));
    }
}
