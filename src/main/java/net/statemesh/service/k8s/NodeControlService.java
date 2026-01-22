package net.statemesh.service.k8s;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.domain.Node;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.node.NodeTask;
import net.statemesh.node.ShellTaskBody;
import net.statemesh.node.TaskBody;
import net.statemesh.repository.NodeRepository;
import net.statemesh.service.exception.OnboardingException;
import net.statemesh.service.mapper.ClusterMapper;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static net.statemesh.config.K8Timeouts.DELETE_NODE_TIMEOUT_SECONDS;

/**
 * Service Implementation for managing {@link Node}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NodeControlService {
    private final NodeRepository nodeRepository;
    private final ClusterMapper clusterMapper;
    private final KubernetesController kubernetesController;
    private final ObjectMapper objectMapper;
    private final AsyncTaskExecutor smTaskExecutor;

    /**
     * Delete the node by id.
     *
     * @param id the id of the entity.
     */
    @Transactional
    public void delete(String id) {
        log.debug("Request to delete Node : {}", id);
        var node = nodeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Node not found")).delete();

        try {
            kubernetesController.deleteNode(
                node.getInternalName(),
                clusterMapper.toDto(node.getCluster(), new CycleAvoidingMappingContext())
            ).get(DELETE_NODE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            smTaskExecutor.submit(() -> {
                try {
                    var shellTask = ShellTaskBody.builder()
                        .command("setsid /usr/local/bin/k3s-agent-uninstall.sh &")
                        .build();
                    var payload = objectMapper.writerFor(ShellTaskBody.class).writeValueAsString(shellTask);
                    new NodeTask(
                        node.getIpv4(),
                        TaskBody.builder()
                            .id("uninstall")
                            .type(TaskBody.TaskTypeShell)
                            .payload(payload)
                            .timeout(0L)
                            .build(),
                        "82a994cc6e06061e13da7ec900c68ddb",
                        false
                    ).call();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            log.error("Could not delete node: {}", node.getInternalName(), e);
            throw new OnboardingException("Could not delete node: " + node.getInternalName());
        }
    }
}
