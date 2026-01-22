package net.statemesh.k8s.flow;

import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.service.ClusterService;
import net.statemesh.service.ResourceService;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.dto.DatabaseDTO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.*;

import static net.statemesh.config.K8Timeouts.*;

@Component
@Slf4j
public class CreateDatabaseFlow extends BaseDatabaseFlow {

    public CreateDatabaseFlow(
        KubernetesController kubernetesController,
        ClusterService clusterService,
        ResourceService resourceService,
        @Qualifier("flowScheduler") ThreadPoolTaskScheduler taskScheduler) {
        super(kubernetesController, clusterService, resourceService, taskScheduler);
    }

    @Override
    public TaskResult<String> execute(DatabaseDTO database) throws K8SException {
        final var cluster = setCluster(database, kubernetesController, clusterService);
        setPublicHostname(database, cluster, kubernetesController);

        try {
            // Ensure the namespace exists
            ensureNamespace(database, cluster).get(CREATE_NAMESPACE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Create PostgreSQL cluster directly
            var postgresqlFuture = kubernetesController.createPostgreSQLCluster(
                getNamespace(database),
                cluster,
                database,
                resourceService.getUserNodes(database)
            ).orTimeout(CREATE_FLOW_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var result = postgresqlFuture.get();

            if (result.isFailed()) {
                log.error("PostgreSQL cluster creation failed for database: {}", database.getName());
                cleanup(database, cluster).get(DELETE_NAMESPACE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                return TaskResult.<String>fail().cluster(cluster);
            }

            if (result.isWaitTimeout()) {
                log.warn("PostgreSQL cluster creation timed out for database: {}", database.getName());
                return TaskResult.<String>waitTimeout().cluster(cluster);
            }

            // Create services
            createServices(database, cluster).get(CREATE_SERVICE_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Optionally create ingress if hasIngress is true
            String ingressHostname = null;
            if (Boolean.TRUE.equals(database.getHasIngress())) {
                var ingressResult = createIngress(database, cluster).get(CREATE_INGRESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (ingressResult.isSuccess() && !StringUtils.isEmpty(ingressResult.getValue())) {
                    ingressHostname = ingressResult.getValue();
                }
            }

            return TaskResult.<String>success()
                .value(ingressHostname != null ? ingressHostname : result.getValue())
                .cluster(cluster);

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error("Error during database deployment: {}", e.getMessage());
            cleanup(database, cluster);
            return TaskResult.fail();
        }
    }

    @Override
    String selectCluster(DatabaseDTO database, KubernetesController kubernetesController) {
        return kubernetesController.selectClusterForDatabase(
            database.getProject().getZone().getZoneId(),
            database
        );
    }

    // CRITICAL: Override to skip storage class creation for databases
    @Override
    CompletableFuture<Void> createStorageClasses(DatabaseDTO database, ClusterDTO cluster) {
        log.debug("Skipping storage class creation for database {} - PostgreSQL operator handles volumes", database.getName());
        return CompletableFuture.completedFuture(null);
    }

    private CompletableFuture<TaskResult<String>> createIngress(DatabaseDTO database, ClusterDTO cluster) {
        return kubernetesController.createIngressTCP(
            getNamespace(database),
            cluster,
            database.getInternalName(),
            database.getInternalName(),
            5432,
            database.getIngressHostName(),
            List.of("ssh"),
            List.of()
        );
    }

    private CompletableFuture<TaskResult<Void>> cleanup(DatabaseDTO database, ClusterDTO cluster) {
        return kubernetesController.deleteNamespace(getNamespace(database), cluster)
            .orTimeout(DELETE_NAMESPACE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }
}
