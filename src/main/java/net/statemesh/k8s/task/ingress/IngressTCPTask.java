package net.statemesh.k8s.task.ingress;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.options.CreateOptions;
import net.statemesh.k8s.crd.traefik.models.*;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.ingressName;

public class IngressTCPTask extends BaseMutationTask<String> {
    private final Logger log = LoggerFactory.getLogger(IngressTCPTask.class);

    private final String resourceName;
    private final String serviceName;
    private final Integer port;
    private final String publicHostname;
    private final List<String> entrypoints;
    private final List<String> middlewares;

    public IngressTCPTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        String resourceName,
        String serviceName,
        Integer port,
        String publicHostname,
        List<String> entrypoints,
        List<String> middlewares
    ) {
        super(apiStub, taskConfig, namespace);
        this.resourceName = resourceName;
        this.serviceName = serviceName;
        this.port = port;
        this.publicHostname = publicHostname;
        this.entrypoints = entrypoints;
        this.middlewares = middlewares;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<String> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create TCP ingress {} if not exists", ingressName(resourceName, port.toString()));

        if (!tcpIngressExists()) {
            log.debug("Create TCP ingress for resource {}", resourceName);
            getApiStub().getTraefikTCPIngress().create(
                getNamespace(),
                new V1alpha1IngressRouteTCP()
                    .apiVersion(TRAEFIK_GROUP + "/" + TRAEFIK_API_VERSION)
                    .kind(TRAEFIK_INGRESS_TCP_KIND)
                    .metadata(
                        new V1ObjectMeta().name(ingressName(resourceName, port.toString()))
                    )
                    .spec(
                        new V1alpha1IngressRouteTCPSpec()
                            .entryPoints(entrypoints)
                            .routes(
                                Collections.singletonList(
                                    middlewares.isEmpty() ?
                                        routeTCPSpecNoMiddlewares() :
                                        routeTCPSpecNoMiddlewares()
                                            .middlewares(
                                                middlewares.stream()
                                                    .map(middleware ->
                                                        new V1alpha1IngressRouteTCPSpecMiddlewares().name(middleware))
                                                    .collect(Collectors.toList())
                                            )
                                )
                            )
                            .tls(new V1alpha1IngressRouteTCPSpecTls())
                    ),
                new CreateOptions()
            );
        } else {
            log.debug("Skipping TCP ingress {} creation as it exists", ingressName(resourceName, port.toString()));
            throw new SkippedExistsException();
        }

        taskResult.value(publicHostname);
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Ingress TCP :: {} :: wait poll step", getNamespace());
        return tcpIngressExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<String> taskResult, boolean ready) {
        log.info("Ingress TCP {} created successfully [{}]", ingressName(resourceName, port.toString()), ready);
    }

    private V1alpha1IngressRouteTCPSpecRoutes routeTCPSpecNoMiddlewares() {
        return new V1alpha1IngressRouteTCPSpecRoutes()
            .match("HostSNI(`" + publicHostname + "`)")
            .services(
                Collections.singletonList(
                    new V1alpha1IngressRouteTCPSpecServices()
                        .name(serviceName)
                        .port(port)
                )
            );
    }

    private boolean tcpIngressExists() throws ApiException {
        V1alpha1IngressRouteTCPList routes = getApiStub().getTraefikTCPIngress().list(getNamespace()).getObject();
        return routes != null && routes.getItems().stream()
            .anyMatch(ingress -> ingressName(resourceName, port.toString()).equals(ingress.getMetadata().getName()));
    }
}
