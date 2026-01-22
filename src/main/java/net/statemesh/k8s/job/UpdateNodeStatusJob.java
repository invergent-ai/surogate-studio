package net.statemesh.k8s.job;

import io.kubernetes.client.openapi.models.V1Node;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.enumeration.NodeRole;
import net.statemesh.domain.enumeration.NodeStatus;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.k8s.util.NetworkUtils;
import net.statemesh.service.NodeService;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.NodeDTO;
import net.statemesh.service.util.ProfileUtil;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

import static net.statemesh.k8s.util.ApiUtils.nodeRole;
import static net.statemesh.k8s.util.K8SConstants.METADATA_NODE_EXTERNAL_IP;

@Component
@Slf4j
public class UpdateNodeStatusJob extends Job {
    private static final String KUBELET_HEALTH_URL = "https://%s:10250/healthz";
    private static final String KUBELET_PODS_URL = "https://%s:10250/runningpods";
    private static final String KUBELET_LOGS_URL = "https://%s:10250/containerLogs/kube-system/%s/cilium-agent?tail=5";
    private static final Pattern ciliumPodMatcher = Pattern.compile("cilium-[a-zA-Z0-9]*");

    private final NodeService nodeService;
    private final Environment environment;

    public UpdateNodeStatusJob(
        KubernetesController kubernetesController,
        ApplicationProperties applicationProperties,
        NodeService nodeService,
        Environment environment
    ) {
        super(kubernetesController, applicationProperties);
        this.nodeService = nodeService;
        this.environment = environment;
    }

    @Scheduled(cron = "${app.job.update-node-status-schedule}")
    public void updateNodes() {
        multiClusterRun(this::updateNodesForCluster);
    }

    private void updateNodesForCluster(ApiStub apiStub) {
        try {
            kubernetesController
                .listNodes(apiStub.getCoreV1Api())
                .getItems()
                .forEach(this::updateNodeStatus);
        } catch (Exception e) {
            log.warn("Failed to list nodes for cluster {}", apiStub.getCluster().getCid(), e);
        }
    }

    private void updateNodeStatus(V1Node k8Node) {
        if (NodeRole.NODE.equals(nodeRole(k8Node))) {
            nodeService.findByInternalName(Objects.requireNonNull(k8Node.getMetadata()).getName())
                .ifPresentOrElse(
                    nodeDTO -> updateExistingNode(k8Node, nodeDTO),
                    () -> handleNonExistingNode(k8Node)
                );
        }

        if (NodeRole.MASTER.equals(nodeRole(k8Node)) && ProfileUtil.isAppliance(environment)) {
            nodeService.findByInternalName(Objects.requireNonNull(k8Node.getMetadata()).getName())
                .ifPresentOrElse(
                    nodeDTO -> updateExistingNode(k8Node, nodeDTO),
                    () -> handleNonExistingNode(k8Node)
                );
        }
    }

    private void updateExistingNode(V1Node k8Node, @NotNull NodeDTO nodeDTO) {
        var annotations = Objects.requireNonNull(k8Node.getMetadata()).getAnnotations();
        if (annotations == null || !annotations.containsKey(METADATA_NODE_EXTERNAL_IP)) {
            log.warn("No external ip label was present on node {}", nodeDTO.getInternalName());
            return;
        }

        var ip = annotations.get(METADATA_NODE_EXTERNAL_IP);
        if (StringUtils.isEmpty(ip)) {
            log.warn("External ip label was present on node {} but was not set", nodeDTO.getInternalName());
            return;
        }

        if (!nodeReachable(ip)) {
            nodeService.updateStatus(nodeDTO.getId(), NodeStatus.NOT_REACHABLE);
            return;
        }

        if (!kubeletReady(ip, nodeDTO.getCluster())) {
            nodeService.updateStatus(nodeDTO.getId(), NodeStatus.PENDING);
            return;
        }

        if (!kubeletHealthy(ip, nodeDTO.getCluster())) {
            nodeService.updateStatus(nodeDTO.getId(), NodeStatus.KUBELET_NOT_HEALTHY);
            return;
        }

        if (!NodeStatus.READY.equals(nodeDTO.getStatus())) {
            nodeService.updateStartTime(nodeDTO.getId(), Instant.now());
        }

        nodeService.updateStatus(nodeDTO.getId(), NodeStatus.READY);
    }

    private void handleNonExistingNode(V1Node k8Node) {
        // The node is not in our database, should we add it or delete it ? (most likely we should ignore it because this is a dev console)
        log.warn("Node {} is not in our database (dev console)", Objects.requireNonNull(k8Node.getMetadata()).getName());
    }

    private boolean nodeReachable(String ipAddress) {
        return NetworkUtils.ping(ipAddress, 5000);
    }

    private boolean kubeletReady(String ipAddress, ClusterDTO cluster) {
        try {
            var client = NetworkUtils.kubeletClient(cluster);
            var request = new Request.Builder()
                .url(String.format(KUBELET_HEALTH_URL, ipAddress))
                .get()
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    return "ok".equals(response.body().string());
                }
            }
        } catch (Throwable ex) {
            log.warn("Failed to create kubelet https client", ex);
        }

        return false;
    }

    private boolean kubeletHealthy(String ipAddress, ClusterDTO cluster) {
        try {
            var client = NetworkUtils.kubeletClient(cluster);
            var request = new Request.Builder()
                .url(String.format(KUBELET_PODS_URL, ipAddress))
                .get()
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    var json = response.body().string();
                    var matcher = ciliumPodMatcher.matcher(json);
                    while(matcher.find()) {
                        String ciliumPod = "";
                        var pod = matcher.group();
                        if (!pod.contains("agent") && !pod.contains("envoy")) {
                            ciliumPod = pod;
                        }

                        if (!StringUtils.isEmpty(ciliumPod)) {
                            request = new Request.Builder()
                                .url(String.format(KUBELET_LOGS_URL, ipAddress, ciliumPod))
                                .get()
                                .build();

                            try (Response execResponse = client.newCall(request).execute()) {
                                if (execResponse.isSuccessful() && execResponse.body() != null) {
                                    String output = execResponse.body().string();
                                    return !StringUtils.isBlank(output);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ex) {
            log.warn("Failed to create kubelet https client", ex);
        }

        return false;
    }
}
