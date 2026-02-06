package net.statemesh.k8s.task.network;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.*;
import net.statemesh.k8s.util.ApiStub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static net.statemesh.k8s.util.K8SConstants.*;

public class NetworkPolicyTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(NetworkPolicyTask.class);

    public NetworkPolicyTask(ApiStub apiStub,
                             TaskConfig taskConfig,
                             String namespace) {
        super(apiStub, taskConfig, namespace);
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Create network policy for namespace {} if not exists", getNamespace());

        if (!policyExists()) {
            log.debug("Create network policy for namespace {}", getNamespace());
            getApiStub().getNetworkingV1Api()
                .createNamespacedNetworkPolicy(
                    getNamespace(),
                    new V1NetworkPolicy()
                        .metadata(
                            new V1ObjectMeta().name(DEFAULT_NS_NETWORK_POLICY_NAME))
                        .spec(
                            new V1NetworkPolicySpec()
                                .podSelector(
                                    new V1LabelSelector()
                                        .addMatchExpressionsItem(
                                            new V1LabelSelectorRequirement()
                                                .operator(OPERATOR_DOESNOTEXIST)
                                                .key(IGNORE_POLICY_LABEL)
                                        )
                                )
                                .policyTypes(List.of("Egress"))
                                    .policyTypes(List.of("Ingress", "Egress"))
                                    .ingress(Collections.singletonList(
                                        new V1NetworkPolicyIngressRule()
                                            .from(namespacePolicies())
                                    ))
                                .egress(Collections.singletonList(
                                    new V1NetworkPolicyEgressRule()
                                        .to(namespacePolicies())
                                        .addToItem(
                                            new V1NetworkPolicyPeer()
                                                .ipBlock(new V1IPBlock().cidr("0.0.0.0/0"))
                                        )
                                ))
                        )
                )
                .execute();
        } else {
            log.debug("Skipping policy for namespace {} creation as it exists", getNamespace());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Network policy for namespace :: {} :: wait poll step", getNamespace());
        return policyExists();
    }

    @Override
    protected void onSuccess(TaskResult.TaskResultBuilder<Void> taskResult, boolean ready) {
        log.info("Network policy for namespace {} created successfully [{}]", getNamespace(), ready);
    }

    private List<V1NetworkPolicyPeer> namespacePolicies() {
        var allowedNamespaces = new ArrayList<>(NETWORK_POLICY_ALLOWED_NAMESPACES);
        allowedNamespaces.add(getNamespace());
        return new ArrayList<>(allowedNamespaces.stream()
            .map(ns -> new V1NetworkPolicyPeer().namespaceSelector(
                new V1LabelSelector().matchLabels(
                    Map.of(NAMESPACE_SELECTOR_LABEL_NAME, ns)
                )
            )).toList());
    }

    private boolean policyExists() throws ApiException {
        try {
            V1NetworkPolicyList policies =
                getApiStub().getNetworkingV1Api().listNamespacedNetworkPolicy(getNamespace()).execute();
            return policies.getItems().stream()
                .anyMatch(policy -> DEFAULT_NS_NETWORK_POLICY_NAME
                    .equals(Objects.requireNonNull(policy.getMetadata()).getName()));
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return false;
            }
            throw e;
        }
    }
}
