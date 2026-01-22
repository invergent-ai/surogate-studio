package net.statemesh.k8s.task.ingress;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.crd.traefik.models.V1alpha1IngressRouteList;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.dto.ContainerDTO;
import net.statemesh.service.dto.PortDTO;
import net.statemesh.service.dto.ResourceDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class DeleteIngressTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeleteIngressTask.class);

    private final ResourceDTO resource;
    private final String portName;

    public DeleteIngressTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        ResourceDTO resource,
        String portName
    ) {
        super(apiStub, taskConfig, namespace);
        this.resource = resource;
        this.portName = portName;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Deleting ingress for resource {} if exists", resource.getInternalName());

        if (ingressExists()) {
            log.debug("Delete ingress for resource {}", resource.getInternalName());
            getApiStub().getTraefikIngress().delete(getNamespace(), ingressName());
        } else {
            log.debug("Skipping ingress deletion for resource {} as it does not exist", resource.getInternalName());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Ingress delete :: {} :: wait poll step", resource.getInternalName());
        return !ingressExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Ingress deleted successfully [{}]", ready);
    }

    private boolean ingressExists() {
        final String ingressName = ingressName();
        if (ingressName == null) {
            return Boolean.FALSE;
        }

        V1alpha1IngressRouteList routes =
            getApiStub().getTraefikIngress().list(getNamespace()).getObject();

        return routes.getItems().stream()
            .anyMatch(route -> ingressName.equals(route.getMetadata().getName()));
    }

    private String ingressName() {
        if (portName != null) {
            return NamingUtils.ingressName(resource.getInternalName(), portName);
        }

        return ((ApplicationDTO) resource).getContainers().stream()
            .map(ContainerDTO::getPorts)
            .flatMap(Set::stream)
            .filter(port -> Boolean.TRUE.equals(port.getIngressPort()))
            .map(PortDTO::getName)
            .map(portName -> NamingUtils.ingressName(resource.getInternalName(), portName))
            .findAny()
            .orElse(null);
    }
}
