package net.statemesh.k8s.task.network;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.options.CreateOptions;
import net.statemesh.k8s.crd.traefik.models.V1alpha1Middleware;
import net.statemesh.k8s.crd.traefik.models.V1alpha1MiddlewareList;
import net.statemesh.k8s.crd.traefik.models.V1alpha1MiddlewareSpec;
import net.statemesh.k8s.crd.traefik.models.V1alpha1MiddlewareSpecIpAllowList;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.FirewallEntryDTO;
import net.statemesh.service.dto.PortDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.ipAllowMiddlewareName;

public class IpAllowMiddlewareTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(IpAllowMiddlewareTask.class);

    private final String applicationName;
    private final PortDTO port;
    private final List<FirewallEntryDTO> firewallEntries;

    public IpAllowMiddlewareTask(ApiStub apiStub,
                                 TaskConfig taskConfig,
                                 String namespace,
                                 String applicationName,
                                 PortDTO port,
                                 List<FirewallEntryDTO> firewallEntries) {
        super(apiStub, taskConfig, namespace);
        this.applicationName = applicationName;
        this.port = port;
        this.firewallEntries = firewallEntries;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create IP Allow Middleware {} if not exists", ipAllowMiddlewareName(applicationName, port.getName()));

        if (!middlewareExists()) {
            log.debug("Create IP Allow Middleware for application {} and port {}", applicationName, port.getName());
            getApiStub().getTraefikMiddleware().create(
                getNamespace(),
                new V1alpha1Middleware()
                    .apiVersion(TRAEFIK_GROUP + "/" + TRAEFIK_API_VERSION)
                    .kind(TRAEFIK_MIDDLEWARE_KIND)
                    .metadata(
                        new V1ObjectMeta().name(ipAllowMiddlewareName(applicationName, port.getName()))
                    )
                    .spec(
                        new V1alpha1MiddlewareSpec()
                            .ipAllowList(new V1alpha1MiddlewareSpecIpAllowList()
                                .sourceRange(
                                    firewallEntries.stream()
                                        .map(FirewallEntryDTO::getCidr)
                                        .toList()
                                )
                            )
                    ),
                new CreateOptions()
            );
        } else {
            log.debug("Skipping IP Allow Middleware {} creation as it exists", ipAllowMiddlewareName(applicationName, port.getName()));
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## IP Allow Middleware :: {} :: wait poll step", getNamespace());
        return middlewareExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("IP Allow Middleware {} created successfully [{}]", ipAllowMiddlewareName(applicationName, port.getName()), ready);
    }

    private boolean middlewareExists() throws ApiException {
        V1alpha1MiddlewareList middlewareList = getApiStub().getTraefikMiddleware().list(getNamespace()).getObject();
        return middlewareList != null && middlewareList.getItems().stream()
            .anyMatch(middleware -> ipAllowMiddlewareName(applicationName, port.getName()).equals(middleware.getMetadata().getName()));
    }
}
