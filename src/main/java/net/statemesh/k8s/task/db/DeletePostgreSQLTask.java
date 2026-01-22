package net.statemesh.k8s.task.db;

import io.kubernetes.client.openapi.ApiException;
import net.statemesh.k8s.exception.SkippedExistsException;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import net.statemesh.service.dto.DatabaseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeletePostgreSQLTask extends BaseMutationTask<Void> {
    private final Logger log = LoggerFactory.getLogger(DeletePostgreSQLTask.class);

    private final DatabaseDTO database;

    public DeletePostgreSQLTask(
        ApiStub apiStub,
        TaskConfig taskConfig,
        String namespace,
        DatabaseDTO database
    ) {
        super(apiStub, taskConfig, namespace);
        this.database = database;
    }

    @Override
    protected void execute(TaskResult.TaskResultBuilder<Void> taskResult) throws ApiException, SkippedExistsException {
        log.info("Deleting database {} if exists", database.getInternalName());

        if (databaseExists()) {
            log.debug("Delete database {}", database.getInternalName());
            var response = getApiStub().getPostgreSQL().delete(
                getNamespace(),
                database.getInternalName()
            );

            if (!response.isSuccess()) {
                throw new ApiException(response.getStatus().getCode(), response.getStatus().getMessage());
            }
        } else {
            log.debug("Skipping database {} deletion as it does not exist", database.getInternalName());
            throw new SkippedExistsException();
        }
    }

    @Override
    protected boolean isReady() throws ApiException {
        log.debug("## Database delete :: {} :: wait poll step", database.getInternalName());
        return !databaseExists();
    }

    private boolean databaseExists() {
        return getApiStub().getPostgreSQL().list(getNamespace())
            .getObject().getItems().stream()
            .anyMatch(db -> database.getInternalName().equals(db.getMetadata().getName())
        );
    }
}
