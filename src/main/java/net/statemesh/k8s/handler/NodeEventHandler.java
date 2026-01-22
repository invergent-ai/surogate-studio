package net.statemesh.k8s.handler;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.informer.ResourceEventHandler;
import io.kubernetes.client.openapi.models.V1Node;
import io.kubernetes.client.openapi.models.V1NodeAddress;
import io.kubernetes.client.openapi.models.V1NodeCondition;
import io.kubernetes.client.openapi.models.V1NodeList;
import io.kubernetes.client.util.Yaml;
import net.statemesh.domain.enumeration.*;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.service.NodeReservationService;
import net.statemesh.service.NodeService;
import net.statemesh.service.dto.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.*;

import static net.statemesh.config.Constants.ADMIN_LOGIN;
import static net.statemesh.config.Constants.NODE_BASE_NAME;
import static net.statemesh.k8s.util.ApiUtils.nodeRole;
import static net.statemesh.k8s.util.K8SConstants.*;
import static net.statemesh.k8s.util.ResourceUtil.lowCPUAvailable;
import static net.statemesh.k8s.util.ResourceUtil.lowMemoryAvailable;

public class NodeEventHandler extends BaseEventHandler implements ResourceEventHandler<V1Node> {
    private static final Logger log = LoggerFactory.getLogger(NodeEventHandler.class);
    private final KubernetesController kubernetesController;

    public NodeEventHandler(
        String zoneId,
        ClusterDTO cluster,
        KubernetesController kubernetesController
    ) {
        super(zoneId, cluster);
        this.kubernetesController = kubernetesController;
    }

    @Override
    public void onAdd(V1Node node) {
        log.debug("{} {} - Node {} added", getZoneId(), getCluster().getCid(),
            Objects.requireNonNull(node.getMetadata()).getName());
        kubernetesController.getTransactionTemplate().execute(tx ->
            createUpdateNode(NodeActionType.CREATE, node, null));

    }

    @Override
    public void onUpdate(V1Node oldNode, V1Node newNode) {
        log.trace("{} {} - Node {} updated; new name {}",
            getZoneId(), getCluster().getCid(),
            Objects.requireNonNull(oldNode.getMetadata()).getName(),
            Objects.requireNonNull(newNode.getMetadata()).getName());
        kubernetesController.getTransactionTemplate().executeWithoutResult(tx ->
            createUpdateNode(NodeActionType.UPDATE, oldNode, newNode));
    }

    @Override
    public void onDelete(V1Node node, boolean b) {
        log.debug("{} {} - Node {} deleted", getZoneId(), getCluster().getCid(),
            Objects.requireNonNull(node.getMetadata()).getName());
        kubernetesController.getTransactionTemplate().executeWithoutResult(tx -> deleteNode(node));
    }

    protected NodeDTO createUpdateNode(NodeActionType action, V1Node node, V1Node newNode) {
        if (node == null || StringUtils.isEmpty(Objects.requireNonNull(node.getMetadata()).getName())) {
            log.warn("Node config was missing. Skipping {} event", action);
            return null;
        }

        NodeService nodeService = kubernetesController.getNodeService();
        Optional<NodeDTO> existingNode =
            nodeService.findByInternalNameAndClusterCid(node.getMetadata().getName(), getCluster().getCid());
        if (NodeActionType.CREATE.equals(action)) {
            if (existingNode.isEmpty()) {
                return createNode(node, action);
            } else {
                log.debug("Node {} exists while trying to perform CREATE action; will check for updates",
                    node.getMetadata().getName());
                return updateNode(existingNode.get(), node, newNode);
            }
        } else if (NodeActionType.UPDATE.equals(action)) {
            if (existingNode.isEmpty()) {
                return createNode(newNode, action);
            } else {
                return updateNode(existingNode.get(), node, newNode);
            }
        } else {
            throw new RuntimeException("Unrecognized action type " + action);
        }
    }

    protected NodeDTO createNode(V1Node node, NodeActionType action) {
        final NodeRole role = nodeRole(node);

        NodeReservationService nodeReservationService = kubernetesController.getNodeReservationService();
        NodeReservationDTO reservation = nodeReservationService.getOrCreate(ADMIN_LOGIN);

        final UserDTO user = reservation.getUser();
        if (user == null) {
            log.warn("No user found for node {}. Skipping node creation", node.getMetadata().getName());
            return null;
        }

        var nodeBuilder = NodeDTO.builder()
            .cluster(getCluster())
            .user(user)
            .internalName(node.getMetadata().getName())
            .name(businessName(node.getMetadata().getName(), user))
            .type(nodeType(node))
            .computeType(computeType(node))
            .status(NodeStatus.PENDING)
            .nodeRole(role)
            .creationTime(Instant.now())
            .lastUpdated(Instant.now())
            .publicCloud(Boolean.FALSE)
            .cloudType(null)
            .cloudStatus(null)
            .yaml(Yaml.dump(node))
            .ipv4(safeIP(node.getStatus().getAddresses(), IPv4))
            .ipv6(safeIP(node.getStatus().getAddresses(), IPv6))
            .hostname(safeHostname(node.getStatus().getAddresses()))
            .architecture(node.getStatus().getNodeInfo().getArchitecture())
            .kernelVersion(node.getStatus().getNodeInfo().getKernelVersion())
            .os(node.getStatus().getNodeInfo().getOperatingSystem())
            .osImage(node.getStatus().getNodeInfo().getOsImage())
            .datacenterName(datacenter(node))
            .rayCluster(rayCluster(node))
            .kubeletVersion(node.getStatus().getNodeInfo().getKubeletVersion())
            .zoneMatch(true)
            .resource(nodeResource(node))
            .condition(nodeCondition(node))
            .history(Collections.singleton(
                NodeHistoryDTO.builder()
                    .action(NodeActionType.CREATE)
                    .timeStamp(Instant.now())
                    .build()
            ));

        NodeDTO createdNode = kubernetesController.getNodeService().save(nodeBuilder.build());

        reservation
            .fulfill()
            .setNode(createdNode);

        nodeReservationService.save(reservation);
        notifyUser(createdNode, user, MessageDTO.MessageType.CREATE);

        return createdNode;
    }

    private NodeResourceDTO nodeResource(V1Node node) {
        return NodeResourceDTO.builder()
            .allocatableCpu(
                safeQuantityValue(node.getStatus().getAllocatable(), CPU_METRIC_KEY)
            )
            .allocatableMemory(
                safeQuantityValue(node.getStatus().getAllocatable(), MEMORY_METRIC_KEY)
            )
            .allocatableEphemeralStorage(
                safeQuantityValue(node.getStatus().getAllocatable(), EPHEMERAL_STORAGE_METRIC_KEY)
            )
            .capacityCpu(
                safeQuantityValue(node.getStatus().getCapacity(), CPU_METRIC_KEY)
            )
            .capacityMemory(
                safeQuantityValue(node.getStatus().getCapacity(), MEMORY_METRIC_KEY)
            )
            .capacityEphemeralStorage(
                safeQuantityValue(node.getStatus().getCapacity(), EPHEMERAL_STORAGE_METRIC_KEY)
            )
            .build();
    }

    private NodeConditionDTO nodeCondition(V1Node node) {
        return NodeConditionDTO.builder()
            .memoryPressure(
                safeCondition(node.getStatus().getConditions(), MEMORY_PRESSURE_CONDITION)
            )
            .diskPressure(
                safeCondition(node.getStatus().getConditions(), DISK_PRESSURE_CONDITION)
            )
            .pidPressure(
                safeCondition(node.getStatus().getConditions(), PID_PRESSURE_CONDITION)
            )
            .kubeletNotReady(
                safeCondition(node.getStatus().getConditions(), KUBELET_READY_CONDITION)
            )
            .build();
    }

    protected NodeDTO updateNode(NodeDTO existingNode, V1Node node, V1Node newNode) {
        List<NodeUpdateType> toUpdate = new ArrayList<>();
        List<NodeResourceType> resourceTypes = new ArrayList<>();
        final V1Node updatedNode = newNode != null ? newNode : node;
        if (updatedNode.getMetadata().getLabels().containsKey(METADATA_IGNORE_EVENTS_KEY)) {
            log.debug("Node has ignore events label (reboot process). Skipping UPDATE event");
            return null;
        }

        // Check name changes
        if (!existingNode.getInternalName().equals(updatedNode.getMetadata().getName())) {
            // K8S doesn't allow this now, but it may in the future and NAME changes also on node REBOOT
            toUpdate.add(NodeUpdateType.NAME);
        }

        // Check condition changes
        if (!Objects.equals(existingNode.getCondition().getMemoryPressure(),
            safeCondition(updatedNode.getStatus().getConditions(), MEMORY_PRESSURE_CONDITION)) ||
            !Objects.equals(existingNode.getCondition().getDiskPressure(),
                safeCondition(updatedNode.getStatus().getConditions(), DISK_PRESSURE_CONDITION)) ||
            !Objects.equals(existingNode.getCondition().getPidPressure(),
                safeCondition(updatedNode.getStatus().getConditions(), PID_PRESSURE_CONDITION)) ||
            !Objects.equals(existingNode.getCondition().getKubeletNotReady(),
                safeCondition(updatedNode.getStatus().getConditions(), KUBELET_READY_CONDITION))) {
            toUpdate.add(NodeUpdateType.CONDITION);
        }
        // Check address changes
        if (!Objects.equals(existingNode.getIpv4(), safeIP(updatedNode.getStatus().getAddresses(), IPv4)) ||
            !Objects.equals(existingNode.getIpv6(), safeIP(updatedNode.getStatus().getAddresses(), IPv6)) ||
            !Objects.equals(existingNode.getHostname(), safeHostname(updatedNode.getStatus().getAddresses()))) {
            toUpdate.add(NodeUpdateType.ADDRESS);
        }
        // Check attribute changes
        if (!StringUtils.equals(existingNode.getArchitecture(), updatedNode.getStatus().getNodeInfo().getArchitecture()) ||
            !StringUtils.equals(existingNode.getKernelVersion(), updatedNode.getStatus().getNodeInfo().getKernelVersion()) ||
            !StringUtils.equals(existingNode.getOs(), updatedNode.getStatus().getNodeInfo().getOperatingSystem()) ||
            !StringUtils.equals(existingNode.getOsImage(), updatedNode.getStatus().getNodeInfo().getOsImage()) ||
            !StringUtils.equals(existingNode.getKubeletVersion(), updatedNode.getStatus().getNodeInfo().getKubeletVersion()) ||
            !StringUtils.equals(existingNode.getDatacenterName(), datacenter(updatedNode)) ||
            !StringUtils.equals(existingNode.getRayCluster(), rayCluster(updatedNode)) ||
            !existingNode.getType().equals(nodeType(updatedNode)) ||
            !existingNode.getComputeType().equals(computeType(updatedNode))) {
            toUpdate.add(NodeUpdateType.ATTRIBUTE);
        }

        if (toUpdate.isEmpty()) {
            log.trace("Nothing interesting to update on node {}. Skipping update", existingNode.getInternalName());
            return null;
        }

        Optional<NodeDTO> updated = Optional.empty();
        if (toUpdate.contains(NodeUpdateType.NAME)) {
            updated = updateName(existingNode, updatedNode);
        }
        if (toUpdate.contains(NodeUpdateType.RESOURCE)) {
            if (resourceTypes.contains(NodeResourceType.ALLOCATABLE)) {
                updated = updateResource(updated.orElse(existingNode), updatedNode, NodeResourceType.ALLOCATABLE);
            }
            if (resourceTypes.contains(NodeResourceType.CAPACITY)) {
                updated = updateResource(updated.orElse(existingNode), updatedNode, NodeResourceType.CAPACITY);
            }
        }
        if (toUpdate.contains(NodeUpdateType.CONDITION)) {
            updated = updateCondition(updated.orElse(existingNode), updatedNode);
        }
        if (toUpdate.contains(NodeUpdateType.ADDRESS)) {
            updated = updateAddress(updated.orElse(existingNode), updatedNode);
        }
        if (toUpdate.contains(NodeUpdateType.ATTRIBUTE)) {
            updateAttributes(updated.orElse(existingNode), updatedNode);

            // Update attributes that can be null-ed (partial update doesn't work with null attributes)
            if (!StringUtils.equals(existingNode.getDatacenterName(), datacenter(updatedNode))) {
                kubernetesController.getNodeService().updateDatacenterName(existingNode.getId(), datacenter(updatedNode));
            }
            if (!StringUtils.equals(existingNode.getRayCluster(), rayCluster(updatedNode))) {
                kubernetesController.getNodeService().updateRayCluster(existingNode.getId(), rayCluster(updatedNode));
            }
        }

        return null;
    }

    private Optional<NodeDTO> updateName(NodeDTO existingNode, V1Node newNode) {
        log.trace("Updating node name. Old name {} -> New name {}",
            existingNode.getInternalName(), newNode.getMetadata().getName());
        NodeService nodeRepository = kubernetesController.getNodeService();
        return nodeRepository.partialUpdate(
            existingNode.toBuilder()
                .internalName(newNode.getMetadata().getName())
                .build()
                .addHistory(
                    NodeHistoryDTO.builder()
                        .action(NodeActionType.UPDATE)
                        .updateType(NodeUpdateType.NAME)
                        .newYaml(Yaml.dump(newNode))
                        .timeStamp(Instant.now())
                        .build()
                )
        );
    }

    private Optional<NodeDTO> updateResource(NodeDTO existingNode, V1Node newNode, NodeResourceType resourceType) {
        log.trace("Updating node {} resources", existingNode.getInternalName());
        NodeService nodeService = kubernetesController.getNodeService();
        return nodeService.partialUpdate(
            existingNode
                .resource(nodeResource(newNode))
                .type(nodeType(newNode))
                .addHistory(
                    NodeHistoryDTO.builder()
                        .action(NodeActionType.UPDATE)
                        .updateType(NodeUpdateType.RESOURCE)
                        .resourceType(resourceType)
                        .newYaml(Yaml.dump(newNode))
                        .timeStamp(Instant.now())
                        .build()
                )
        );
    }

    private Optional<NodeDTO> updateCondition(NodeDTO existingNode, V1Node newNode) {
        log.trace("Updating node {} conditions", existingNode.getInternalName());
        NodeService nodeService = kubernetesController.getNodeService();
        return nodeService.partialUpdate(
            existingNode
                .condition(nodeCondition(newNode))
                .addHistory(
                    NodeHistoryDTO.builder()
                        .action(NodeActionType.UPDATE)
                        .updateType(NodeUpdateType.CONDITION)
                        .newYaml(Yaml.dump(newNode))
                        .timeStamp(Instant.now())
                        .build()
                )
        );
    }

    private Optional<NodeDTO> updateAddress(NodeDTO existingNode, V1Node newNode) {
        log.trace("Updating node {} addresses", existingNode.getInternalName());
        NodeService nodeService = kubernetesController.getNodeService();
        return nodeService.partialUpdate(
            existingNode.toBuilder()
                .ipv4(safeIP(newNode.getStatus().getAddresses(), IPv4))
                .ipv6(safeIP(newNode.getStatus().getAddresses(), IPv6))
                .hostname(safeHostname(newNode.getStatus().getAddresses()))
                .build()
                .addHistory(
                    NodeHistoryDTO.builder()
                        .action(NodeActionType.UPDATE)
                        .updateType(NodeUpdateType.ADDRESS)
                        .newYaml(Yaml.dump(newNode))
                        .timeStamp(Instant.now())
                        .build()
                )
        );
    }

    private void updateAttributes(NodeDTO existingNode, V1Node newNode) {
        log.trace("Updating node {} attributes", existingNode.getInternalName());
        NodeService nodeService = kubernetesController.getNodeService();
        nodeService.partialUpdate(
            existingNode.toBuilder()
                .architecture(newNode.getStatus().getNodeInfo().getArchitecture())
                .os(newNode.getStatus().getNodeInfo().getOperatingSystem())
                .osImage(newNode.getStatus().getNodeInfo().getOsImage())
                .datacenterName(datacenter(newNode))
                .rayCluster(rayCluster(newNode))
                .kernelVersion(newNode.getStatus().getNodeInfo().getKernelVersion())
                .kubeletVersion(newNode.getStatus().getNodeInfo().getKubeletVersion())
                .type(nodeType(newNode))
                .computeType(computeType(newNode))
                .build()
                .addHistory(
                    NodeHistoryDTO.builder()
                        .action(NodeActionType.UPDATE)
                        .updateType(NodeUpdateType.ATTRIBUTE)
                        .newYaml(Yaml.dump(newNode))
                        .timeStamp(Instant.now())
                        .build()
                )
        );
    }

    protected void deleteNode(V1Node node) {
        if (node == null || StringUtils.isEmpty(node.getMetadata().getName())) {
            log.warn("Node config was missing. Skipping DELETE event");
            return;
        }
        if (node.getMetadata().getLabels().containsKey(METADATA_IGNORE_EVENTS_KEY)) {
            log.debug("Node has ignore events label (reboot process). Skipping DELETE event");
            return;
        }

        NodeService nodeService = kubernetesController.getNodeService();
        Optional<NodeDTO> existingNode =
            nodeService.findByInternalNameAndClusterCid(node.getMetadata().getName(), getCluster().getCid());
        if (existingNode.isPresent()) {
            NodeDTO deleted = nodeService.save(
                existingNode.get().toBuilder()
                    .internalName(deletedName(existingNode))
                    .build()
                    .delete()
                    .addHistory(
                        NodeHistoryDTO.builder()
                            .action(NodeActionType.DELETE)
                            .timeStamp(Instant.now())
                            .build()
                    )
            );

            notifyUser(deleted, deleted.getUser(), MessageDTO.MessageType.DELETE);
        } else {
            log.error("Node {} was not found for deletion", node.getMetadata().getName());
        }
    }

    private void notifyUser(NodeDTO node, UserDTO user, MessageDTO.MessageType type) {
        log.debug("Notifying user {} of node {} creation", user.getLogin(), node.getName());
        SimpMessagingTemplate messagingTemplate =
            kubernetesController.getSimpMessagingTemplate();
        messagingTemplate.convertAndSend("/topic/message/" + user.getLogin(),
            MessageDTO.builder()
                .type(type)
                .nodes(Collections.singletonList(node))
                .build()
        );
    }

    private BigDecimal safeQuantityValue(Map<String, Quantity> map, String key) {
        return map.containsKey(key) ?
            map.get(key) != null ? map.get(key).getNumber() : null : null;
    }

    private Boolean safeCondition(List<V1NodeCondition> conditions, String type) {
        return conditions.stream()
            .filter(condition -> type.equals(condition.getType()))
            .findAny()
            .map(condition -> Boolean.parseBoolean(condition.getStatus()))
            .orElse(Boolean.FALSE);
    }

    private String safeHostname(List<V1NodeAddress> addresses) {
        return addresses.stream()
            .filter(address -> HOSTNAME_ADDRESS.equals(address.getType()))
            .findAny()
            .map(V1NodeAddress::getAddress)
            .orElse(null);
    }

    private String safeIP(List<V1NodeAddress> addresses, String version) {
        return addresses.stream()
            .filter(address -> INTERNAL_IP_ADDRESS.equals(address.getType()))
            .map(V1NodeAddress::getAddress)
            .filter(ip -> !StringUtils.isEmpty(ip))
            .map(this::safeIP)
            .filter(Objects::nonNull)
            .filter(inet -> IPv4.equals(version) ?
                inet instanceof Inet4Address : inet instanceof Inet6Address)
            .map(InetAddress::getHostAddress)
            .findAny()
            .orElse(null);
    }

    private InetAddress safeIP(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            // Ignore
        }
        return null;
    }

    private String businessName(String internalName, UserDTO user) {
        if (internalName == null) {
            return Strings.EMPTY;
        }

        String suffix = internalName.split("\\.")[0].split("-")[0];
        return StringUtils.join(generatePrettyName(user), suffix.substring(0, Math.min(suffix.length(), 4)));
    }

    private NodeType nodeType(V1Node node) {
        // Simple temp. check - we can add a more complex mechanism based on benchmark,network or KubeEdge
        final V1NodeList nodes = new V1NodeList().items(Collections.singletonList(node));
        return node.getMetadata().getLabels().containsKey(EDGE_NODE_LABEL) || lowCPUAvailable(nodes) || lowMemoryAvailable(nodes)
            ? NodeType.EDGE : NodeType.NODE;
    }

    private ComputeType computeType(V1Node node) {
        return node.getMetadata().getLabels().containsKey(HAS_GPU_LABEL) ? ComputeType.GPU : ComputeType.CPU;
    }

    private String datacenter(V1Node node) {
        return node.getMetadata().getLabels().getOrDefault(DATACENTER_NAME_LABEL, null);
    }

    private String rayCluster(V1Node node) {
        return node.getMetadata().getLabels().getOrDefault(RAY_CLUSTER_NAME_LABEL, null);
    }

    private String generatePrettyName(UserDTO user) {
        long current = kubernetesController.getNodeService().countForUser(user) + 1;
        return NODE_BASE_NAME + current + "-";
    }

    private String deletedName(Optional<NodeDTO> node) {
        return node.map(n -> n.getInternalName() + "_" + RandomStringUtils.secure().nextAlphanumeric(10))
            .orElse(null);
    }
}
