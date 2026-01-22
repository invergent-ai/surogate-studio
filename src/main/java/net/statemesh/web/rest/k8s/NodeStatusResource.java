package net.statemesh.web.rest.k8s;

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
public class NodeStatusResource {
    private final Logger log = LoggerFactory.getLogger(NodeStatusResource.class);
    private final NodeStatusService nodeStatusService;

    public NodeStatusResource(NodeStatusService nodeStatusService) {
        this.nodeStatusService = nodeStatusService;
    }

    @GetMapping("/status/{nodeId}")
    public ResponseEntity<Void> startStatus(@PathVariable(name = "nodeId") String nodeId) {
        log.debug("REST request to start status for node {}", nodeId);
        nodeStatusService.startStatus(nodeId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/status/{nodeId}")
    public ResponseEntity<Void> stopStatus(@PathVariable(name = "nodeId") String nodeId) {
        nodeStatusService.stopStatus(nodeId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/snapshot/{clusterCid}")
    public ResponseEntity<Set<NodeStatsDTO>> getStatusSnapshot(@PathVariable(name = "clusterCid") String clusterCid) {
        try {
            return ResponseEntity.ok(
                nodeStatusService.statusSnapshotForNodes(clusterCid));
        } catch (Exception e) {
            log.error("Error getting status snapshot for cluster {}", clusterCid, e);
            return ResponseEntity.ok(Collections.emptySet());
        }
    }

    @GetMapping("/node-snapshot/{nodeId}")
    public ResponseEntity<NodeStatsDTO> getNodeStatusSnapshot(@PathVariable(name = "nodeId") String nodeId) {
        try {
            return ResponseEntity.ok(
                nodeStatusService.statusSnapshotForNode(nodeId));
        } catch (Exception e) {
            log.error("Error getting status snapshot for node {}", nodeId, e);
            return ResponseEntity.ok(null);
        }
    }
}
