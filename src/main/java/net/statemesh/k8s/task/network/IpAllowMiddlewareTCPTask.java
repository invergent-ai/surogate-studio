package net.statemesh.k8s.task.network;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.util.generic.options.CreateOptions;
import net.statemesh.k8s.crd.traefik.models.V1alpha1MiddlewareTCP;
import net.statemesh.k8s.crd.traefik.models.V1alpha1MiddlewareTCPList;
import net.statemesh.k8s.crd.traefik.models.V1alpha1MiddlewareTCPSpec;
import net.statemesh.k8s.crd.traefik.models.V1alpha1MiddlewareTCPSpecIpAllowList;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.FirewallEntryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.NamingUtils.ipAllowMiddlewareName;

public class IpAllowMiddlewareTCPTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(IpAllowMiddlewareTCPTask.class);

    private final String resourceName;
    private final Integer port;
    private final List<FirewallEntryDTO> firewallEntries;

    public IpAllowMiddlewareTCPTask(ApiStub apiStub,
                                    TaskConfig taskConfig,
                                    String namespace,
                                    String resourceName,
                                    Integer port,
                                    List<FirewallEntryDTO> firewallEntries) {
        super(apiStub, taskConfig, namespace);
        this.resourceName = resourceName;
        this.port = port;
        this.firewallEntries = firewallEntries;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create IP Allow Middleware TCP {} if not exists", ipAllowMiddlewareName(resourceName, port.toString()));

        if (!middlewareTCPExists()) {
            log.debug("Create IP Allow Middleware TCP for resource {} and port {}", resourceName, port);
            getApiStub().getTraefikMiddlewareTCP().create(
                getNamespace(),
                new V1alpha1MiddlewareTCP()
                    .apiVersion(TRAEFIK_GROUP + "/" + TRAEFIK_API_VERSION)
                    .kind(TRAEFIK_MIDDLEWARE_TCP_KIND)
                    .metadata(
                        new V1ObjectMeta().name(ipAllowMiddlewareName(resourceName, port.toString()))
                    )
                    .spec(
                        new V1alpha1MiddlewareTCPSpec()
                            .ipAllowList(new V1alpha1MiddlewareTCPSpecIpAllowList()
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
            log.debug("Skipping IP Allow Middleware TCP {} creation as it exists", ipAllowMiddlewareName(resourceName, port.toString()));
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## IP Allow Middleware TCP :: {} :: wait poll step", getNamespace());
        return middlewareTCPExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("IP Allow Middleware TCP {} created successfully [{}]", ipAllowMiddlewareName(resourceName, port.toString()), ready);
    }

    private boolean middlewareTCPExists() throws ApiException {
        V1alpha1MiddlewareTCPList middlewareList = getApiStub().getTraefikMiddlewareTCP().list(getNamespace()).getObject();
        return middlewareList != null && middlewareList.getItems().stream()
            .anyMatch(middleware ->
                ipAllowMiddlewareName(resourceName, port.toString()).equals(middleware.getMetadata().getName()));
    }
}
