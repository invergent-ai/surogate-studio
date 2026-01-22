package net.statemesh.service;

import io.kubernetes.client.openapi.ApiException;
import lombok.RequiredArgsConstructor;
import net.statemesh.domain.Database;
import net.statemesh.domain.enumeration.DatabaseStatus;
import net.statemesh.domain.enumeration.ProcessEvent;
import net.statemesh.domain.enumeration.Profile;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.flow.CreateDatabaseFlow;
import net.statemesh.k8s.flow.DeleteDatabaseFlow;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ResourceStatus;
import net.statemesh.repository.DatabaseRepository;
import net.statemesh.service.dto.*;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.DatabaseMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.statemesh.config.Constants.APP_ERROR_TECHNICAL_KEY;
import static net.statemesh.k8s.util.ApiUtils.readDatabasePassword;
import static net.statemesh.k8s.util.NamingUtils.resourceName;
import static net.statemesh.service.util.ProjectUtil.updateProject;

@Service
@Transactional
@RequiredArgsConstructor
public class DatabaseService {
    private final Logger log = LoggerFactory.getLogger(DatabaseService.class);

    private final CreateDatabaseFlow createDatabaseFlow;
    private final KubernetesController kubernetesController;
    private final DeleteDatabaseFlow deleteDatabaseFlow;
    private final DatabaseRepository databaseRepository;
    private final DatabaseMapper databaseMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final ResourceService resourceService;

    @Transactional
    public DatabaseDTO save(DatabaseDTO databaseDTO, String login) {
        log.debug("Request to save Database : {}", databaseDTO);
        if (StringUtils.isEmpty(databaseDTO.getInternalName())) {
            // It is of utmost importance that this happens only once on db creation
            databaseDTO.setInternalName(resourceName(databaseDTO.getName()));
        }

        var database = databaseMapper.toDto(
            databaseRepository.save(updateRelationships(databaseDTO)),
            new CycleAvoidingMappingContext()
        );

        notifyUser(database, login,
            StringUtils.isEmpty(databaseDTO.getId()) ?
                MessageDTO.MessageType.CREATE :
                MessageDTO.MessageType.UPDATE
        );


        return database;
    }

    @Transactional
    public DatabaseDTO deploy(DatabaseDTO database, String login) {


        try {
            TaskResult<String> result = this.createDatabaseFlow.executeCreate(database);

            database.setStatus(result.isSuccess() ? DatabaseStatus.DEPLOYED : DatabaseStatus.DEPLOYING);
            database.setIngressHostName(Optional.ofNullable(result.getValue()).map(Object::toString).orElse(null));
            database.setDeployedNamespace(database.getProject().getNamespace());
            database.getProject().setCluster(result.getCluster());

            notificationService.notifyUser(database, login,
                result.isSuccess() ? ProcessEvent.DEPLOYED : ProcessEvent.PENDING, null, false);
        } catch (K8SException e) {
            log.error("Failed to deploy database", e);
            database.setStatus(DatabaseStatus.ERROR);
            database.setErrorMessageKey(APP_ERROR_TECHNICAL_KEY);

            updateProject(projectService, database);
        }

        return save(database, login);
    }

    @Transactional
    public DatabaseDTO redeploy(DatabaseDTO database, String login) {
        deleteDatabaseFlow.executeDelete(database);
        return deploy(database, login);
    }

    public Stream<ResourceCostDTO> prepareOpenCostRequests() {
        return databaseRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(ResourceCostDTO::fromDb)
            .filter(app -> !StringUtils.isEmpty(app.getClusterId()));
    }

    private Database updateRelationships(DatabaseDTO databaseDTO) {
        updateProject(projectService, databaseDTO);
        final Database db = databaseMapper.toEntity(databaseDTO, new CycleAvoidingMappingContext());
        db.getVolumeMounts().forEach(volumeMount -> {
            volumeMount.setDatabase(db);
            if (db.getProject() != null) {
                volumeMount.getVolume().setProject(db.getProject());
            }
        });
        db.getFirewallEntries().forEach(firewallEntry -> firewallEntry.setDatabase(db));

        return db;
    }

    @Transactional(readOnly = true)
    public List<DatabaseDTO> searchByName(String query) {
        log.debug("Request to search Applications by name containing : {}", query);
        return databaseRepository.findByNameContainingIgnoreCase(query)
            .stream()
            .map(app -> databaseMapper.toDto(app, new CycleAvoidingMappingContext()))
            .collect(Collectors.toList());
    }

    public String getDatabasePassword(DatabaseDTO database) {
        if (database == null || StringUtils.isEmpty(database.getInternalName())
            || StringUtils.isEmpty(database.getDeployedNamespace())
            || database.getProject() == null || database.getProject().getCluster() == null) {
            throw new RuntimeException("Bad database object. Could not read password");
        }

        try {
            return readDatabasePassword(
                kubernetesController.getApi(database.getProject().getCluster()),
                database
            );
        } catch (ApiException e) {
            log.error("Could not read database password with message: {}", e.getMessage());
        }
        return StringUtils.EMPTY;
    }

    /**
     * Get all the databases.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<DatabaseDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Databases");
        return databaseRepository.findAll(pageable)
            .map(o -> databaseMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the databases with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<DatabaseDTO> findAllWithEagerRelationships(Pageable pageable) {
        return databaseRepository.findAllWithEagerRelationships(pageable)
            .map(o -> databaseMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    public List<DatabaseDTO> findUserDatabases(String login) {
        return databaseRepository.findUserDatabases(login).stream()
            .map(db -> databaseMapper.toDto(db, new CycleAvoidingMappingContext()))
            .toList();
    }

    /**
     * Get one database by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<DatabaseDTO> findOne(String id) {
        log.debug("Request to get Database : {}", id);
        return databaseRepository.findOneWithEagerRelationships(id)
            .map(o -> databaseMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the database by id.
     *
     * @param id the id of the entity.
     */
    @Transactional
    public void delete(String id, String login, boolean keepVolumes) {
        log.debug("Request to delete Database : {}", id);
        Optional<Database> database = databaseRepository.findById(id);
        if (database.isPresent()) {
            resourceService.detachVolumesInUse(database.get().getVolumeMounts(), keepVolumes);

            final DatabaseDTO databaseDTO =
                databaseMapper.toDto(database.get(), new CycleAvoidingMappingContext());

            databaseRepository.deleteById(id);
            deleteDatabaseFlow.executeDelete(databaseDTO);

            notifyUser(databaseDTO, login,  MessageDTO.MessageType.DELETE);
            notificationService.notifyUser(databaseDTO, login, ProcessEvent.DELETED, null, false);
        }
    }


    @Transactional
    public void updateStatus(String id, DatabaseStatus status) {
        databaseRepository.updateStatus(id, status);
    }

    @Transactional
    public void updateStage(String id, ResourceStatus.ResourceStatusStage stage) {
        databaseRepository.updateStage(id, stage);
    }

    public boolean userHasDatabases(UserDTO user) {
        return databaseRepository.countUserDatabases(user.getLogin()) > 0;
    }

    public void notifyUser(DatabaseDTO database, String login, MessageDTO.MessageType type) {
        log.debug("Notifying user {} of database {} update type {}", login, database.getName(), type);
        messagingTemplate.convertAndSend("/topic/message/" + login,
            MessageDTO.builder()
                .type(type)
                .dbs(Collections.singletonList(database))
                .build()
        );
    }
}
