package net.statemesh.k8s.task.application;

import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.service.dto.PortDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import static net.statemesh.k8s.util.K8SConstants.DEFAULT_SERVICE_PROTOCOL;
import static net.statemesh.k8s.util.K8SConstants.DEFAULT_SERVICE_TYPE;
import static net.statemesh.k8s.util.NamingUtils.portNameLimit;
import static net.statemesh.k8s.util.NamingUtils.serviceName;

public class ServiceTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(ServiceTask.class);

    private final String applicationName;
    private final PortDTO port;
    private final Map<String, String> selectors;

    public ServiceTask(ApiStub apiStub,
                       TaskConfig taskConfig,
                       String namespace,
                       String applicationName,
                       PortDTO port,
                       Map<String, String> selectors) {
        super(apiStub, taskConfig, namespace);
        this.applicationName = applicationName;
        this.port = port;
        this.selectors = selectors == null ? NamingUtils.appLabels(applicationName) : selectors;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create service {} if not exists", serviceName(applicationName, port.getName()));

        if (!serviceExists()) {
            log.debug("Create service for application {} and port {}", applicationName, port.getName());
            getApiStub().getCoreV1Api()
                .createNamespacedService(
                    getNamespace(),
                    new V1Service()
                        .metadata(
                            new V1ObjectMeta()
                                .name(serviceName(applicationName, port.getName()))
                                .labels(this.selectors)
                        )
                        .spec(
                            new V1ServiceSpec()
                                .type(DEFAULT_SERVICE_TYPE)
                                .selector(this.selectors)
                                .ports(
                                    Collections.singletonList(
                                        new V1ServicePort()
                                            .name(portNameLimit(port.getName()))
                                            .port(port.getServicePort())
                                            .targetPort(
                                                port.getTargetPort() != null ?
                                                    new IntOrString(port.getTargetPort()) :
                                                    new IntOrString(portNameLimit(port.getName()))
                                            )
                                            .protocol(
                                                port.getProtocol() != null ?
                                                    port.getProtocol().getCode() : DEFAULT_SERVICE_PROTOCOL
                                            )
                                    )
                                )
                        )
                )
                .execute();
        } else {
            log.debug("Skipping service {} creation as it exists", serviceName(applicationName, port.getName()));
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Service :: {} :: wait poll step", getNamespace());
        return serviceExists();
    }

    private boolean serviceExists() throws ApiException {
        try {
            V1ServiceList services =
                getApiStub().getCoreV1Api().listNamespacedService(getNamespace()).execute();
            return services.getItems().stream()
                .anyMatch(service -> serviceName(applicationName,
                    port.getName()).equals(service.getMetadata().getName()));
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
