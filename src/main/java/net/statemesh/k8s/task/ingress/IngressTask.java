package net.statemesh.k8s.task.ingress;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.options.CreateOptions;
import net.statemesh.k8s.crd.traefik.models.*;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.PortDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.ingressName;
import static net.statemesh.k8s.util.NamingUtils.serviceName;

public class IngressTask extends BaseMutationTask<String> {
    private final Logger log = LoggerFactory.getLogger(IngressTask.class);

    private final String publicHostname;
    private final String applicationName;
    private final PortDTO port;
    private final List<String> middlewares;

    public IngressTask(ApiStub apiStub,
                       TaskConfig taskConfig,
                       String namespace,
                       String publicHostname,
                       String applicationName,
                       PortDTO port,
                       List<String> middlewares) {
        super(apiStub, taskConfig, namespace);
        this.publicHostname = publicHostname;
        this.applicationName = applicationName;
        this.port = port;
        this.middlewares = middlewares;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<String> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create Traefik ingress {} if not exists", ingressName(applicationName, port.getName()));

        if (!ingressExists()) {
            log.debug("Create Traefik ingress for application {} and port {}", applicationName, port.getName());
            getApiStub().getTraefikIngress().create(
                getNamespace(),
                new V1alpha1IngressRoute()
                    .apiVersion(TRAEFIK_GROUP + "/" + TRAEFIK_API_VERSION)
                    .kind(TRAEFIK_INGRESS_ROUTE_KIND)
                    .metadata(
                        new V1ObjectMeta().name(ingressName(applicationName, port.getName()))
                    )
                    .spec(routeSpec()),
                new CreateOptions()
            );
        } else {
            log.debug("Skipping ingress {} creation as it exists", ingressName(applicationName, port.getName()));
            throw new SkippedExistsException();
        }

        taskResult.value(publicHostname);
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Ingress :: {} :: wait poll step", getNamespace());
        return ingressExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<String> taskResult, boolean ready) {
        log.info("Ingress {} created successfully [{}]", ingressName(applicationName, port.getName()), ready);
    }

    private V1alpha1IngressRouteSpec routeSpec() {
        return StringUtils.isEmpty(port.getIngressHost()) ?
            routeSpecNoACME() :
            routeSpecNoACME()
                .tls(new V1alpha1IngressRouteSpecTls().certResolver(TRAEFIK_ACME_CERTRESOLVER));
    }

    private V1alpha1IngressRouteSpec routeSpecNoACME() {
        return new V1alpha1IngressRouteSpec()
            .entryPoints(TRAEFIK_DEFAULT_ENTRYPOINTS)
            .routes(
                Collections.singletonList(
                    middlewares.isEmpty() ?
                        routeSpecRouteNoMiddlewares() :
                        routeSpecRouteNoMiddlewares()
                            .middlewares(
                                middlewares.stream()
                                    .map(middleware -> new V1alpha1IngressRouteSpecMiddlewares().name(middleware))
                                    .collect(Collectors.toList())
                            )
                )
            );
    }

    private V1alpha1IngressRouteSpecRoutes routeSpecRouteNoMiddlewares() {
        return new V1alpha1IngressRouteSpecRoutes()
            .kind(V1alpha1IngressRouteSpecRoutes.KindEnum.RULE)
            .match("Host(`" + publicHostname + "`)")
            .services(
                Collections.singletonList(
                    new V1alpha1IngressRouteSpecServices()
                        .kind(V1alpha1IngressRouteSpecServices.KindEnum.SERVICE)
                        .name(serviceName(applicationName, port.getName()))
                        .port(port.getServicePort())
                )
            );
    }

    protected Map<String, String> selector() {
        return Map.of(SERVICE_SELECTOR_LABEL_NAME, applicationName);
    }

    private boolean ingressExists() {
        V1alpha1IngressRouteList routes = getApiStub().getTraefikIngress().list(getNamespace()).getObject();
        return routes != null && routes.getItems().stream()
            .anyMatch(ingress -> ingressName(applicationName, port.getName()).equals(ingress.getMetadata().getName()));
    }
}
