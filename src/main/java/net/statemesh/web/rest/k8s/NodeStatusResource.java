package net.statemesh.web.rest.k8s;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.statemesh.service.dto.NodeStatsDTO;
import net.statemesh.service.k8s.status.NodeStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.Set;

@Controller
@RequestMapping("/api/status/node")
@Tag(name = "Node Status", description = "Kubernetes node status and resource snapshots")
public class NodeStatusResource {
    private final Logger log = LoggerFactory.getLogger(NodeStatusResource.class);
    private final NodeStatusService nodeStatusService;

    public NodeStatusResource(NodeStatusService nodeStatusService) {
        this.nodeStatusService = nodeStatusService;
    }

    @Operation(summary = "Start node status polling")
    @ApiResponse(responseCode = "200", description = "Node status polling started")
    @GetMapping("/status/{nodeId}")
    public ResponseEntity<Void> startStatus(
        @Parameter(description = "Node ID") @PathVariable(name = "nodeId") String nodeId) {
        log.debug("REST request to start status for node {}", nodeId);
        nodeStatusService.startStatus(nodeId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Stop node status polling")
    @ApiResponse(responseCode = "200", description = "Node status polling stopped")
    @DeleteMapping("/status/{nodeId}")
    public ResponseEntity<Void> stopStatus(
        @Parameter(description = "Node ID") @PathVariable(name = "nodeId") String nodeId) {
        nodeStatusService.stopStatus(nodeId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get cluster node status snapshot", description = "Get resource usage snapshot for all nodes in a cluster")
    @ApiResponse(responseCode = "200", description = "Snapshot returned")
    @GetMapping("/snapshot/{clusterCid}")
    public ResponseEntity<Set<NodeStatsDTO>> getStatusSnapshot(
        @Parameter(description = "Cluster CID") @PathVariable(name = "clusterCid") String clusterCid) {
        try {
            return ResponseEntity.ok(nodeStatusService.statusSnapshotForNodes(clusterCid));
        } catch (Exception e) {
            log.error("Error getting status snapshot for cluster {}", clusterCid, e);
            return ResponseEntity.ok(Collections.emptySet());
        }
    }

    @Operation(summary = "Get single node status snapshot", description = "Get resource usage snapshot for a specific node")
    @ApiResponse(responseCode = "200", description = "Node snapshot returned")
    @GetMapping("/node-snapshot/{nodeId}")
    public ResponseEntity<NodeStatsDTO> getNodeStatusSnapshot(
        @Parameter(description = "Node ID") @PathVariable(name = "nodeId") String nodeId) {
        try {
            return ResponseEntity.ok(nodeStatusService.statusSnapshotForNode(nodeId));
        } catch (Exception e) {
            log.error("Error getting status snapshot for node {}", nodeId, e);
            return ResponseEntity.ok(null);
        }
    }
}
