package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.domain.Application;
import net.statemesh.domain.Container;
import net.statemesh.domain.Volume;
import net.statemesh.domain.enumeration.ApplicationStatus;
import net.statemesh.domain.enumeration.ProcessEvent;
import net.statemesh.k8s.exception.K8SException;
import net.statemesh.k8s.flow.CreateApplicationFlow;
import net.statemesh.k8s.flow.CreateModelFlow;
import net.statemesh.k8s.flow.DeleteApplicationFlow;
import net.statemesh.k8s.flow.DeleteModelFlow;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.repository.ApplicationRepository;
import net.statemesh.repository.VolumeMountRepository;
import net.statemesh.repository.VolumeRepository;
import net.statemesh.service.dto.*;
import net.statemesh.service.mapper.ApplicationMapper;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.statemesh.k8s.util.NamingUtils.resourceName;
import static net.statemesh.service.util.ProjectUtil.updateProject;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Application}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApplicationService {
    private final CreateApplicationFlow createApplicationFlow;
    private final CreateModelFlow createModelFlow;
    private final DeleteApplicationFlow deleteApplicationFlow;
    private final ApplicationRepository applicationRepository;
    private final ApplicationMapper applicationMapper;
    private final VolumeRepository volumeRepository;
    private final VolumeMountRepository volumeMountRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ProjectService projectService;
    private final NotificationService notificationService;
    private final ResourceService resourceService;
    private final DeleteModelFlow deleteModelFlow;
    private final TransactionTemplate transactionTemplate;

    public ApplicationDTO save(ApplicationDTO applicationDTO, String login) {
        log.debug("Request to save Application : {}", applicationDTO);
        if (StringUtils.isEmpty(applicationDTO.getInternalName())) {
            // It is of utmost importance that this happens only once on app creation
            applicationDTO.setInternalName(resourceName(applicationDTO.getName()));
        }

        var app = transactionTemplate.execute(tx -> {
            var _innerApplication = applicationMapper.toDto(
                applicationRepository.save(updateRelationships(applicationDTO)),
                new CycleAvoidingMappingContext()
            );

            keepTransient(_innerApplication, applicationDTO);
            return _innerApplication;
        });

        notifyUser(app, login,
            StringUtils.isEmpty(applicationDTO.getId()) ?
                MessageDTO.MessageType.CREATE :
                MessageDTO.MessageType.UPDATE
        );

        return app;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApplicationDTO deploy(ApplicationDTO application, String login) {
        try {
            application.setStatus(ApplicationStatus.DEPLOYING);
            updateStatus(application.getId(), ApplicationStatus.DEPLOYING);

            TaskResult<String> result = switch (application.getMode()) {
                case APPLICATION -> createApplicationFlow.execute(application);
                case MODEL -> createModelFlow.execute(application);
            };
            application.setIngressHostName(result.getValue());
            application.setYamlConfig(result.getValue());
            application.setDeployedNamespace(application.getProject().getNamespace());
            application.getProject().setCluster(result.getCluster());

            notificationService.notifyUser(application, login,
                result.isSuccess() ? ProcessEvent.DEPLOYED : ProcessEvent.PENDING, null, false);
        } catch (K8SException e) {
            log.error("Failed to deploy application", e);
            application.setStatus(ApplicationStatus.ERROR);
            application.setErrorMessageKey(e.getMessage());
            updateProject(projectService, application);
        }

        return save(application, login);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ApplicationDTO redeploy(ApplicationDTO application, String login) {
        application.setStatus(ApplicationStatus.DELETING);
        updateStatus(application.getId(), ApplicationStatus.DELETING);

        switch (application.getMode()) {
            case APPLICATION -> deleteApplicationFlow.execute(application);
            case MODEL -> deleteModelFlow.execute(application);
        }

        application.setStatus(ApplicationStatus.CREATED);
        updateStatus(application.getId(), ApplicationStatus.CREATED);

        return deploy(application, login);
    }

    @Transactional(readOnly = true)
    public Stream<ResourceCostDTO> prepareOpenCostRequests() {
        return applicationRepository
            .findAllWithEagerRelationships()
            .stream()
            .map(ResourceCostDTO::fromApp)
            .filter(app -> !StringUtils.isEmpty(app.getClusterId()));
    }

    private Application updateRelationships(ApplicationDTO applicationDTO) {
        updateProject(projectService, applicationDTO);
        final Application app = applicationMapper.toEntity(applicationDTO, new CycleAvoidingMappingContext());
        app.getContainers().forEach(container -> {
            container.setApplication(app);
            container.getPorts().forEach(port -> port.setContainer(container));
            container.getProbes().forEach(probe -> probe.setContainer(container));
            container.getEnvVars().forEach(envVar -> envVar.setContainer(container));
            container.getFirewallEntries().forEach(entry -> entry.setContainer(container));
            container.getVolumeMounts().forEach(volumeMount -> {
                volumeMount.setContainer(container);
                if (!StringUtils.isEmpty(volumeMount.getVolume().getId())) {
                    volumeMount.setVolume(
                        mergeVolume(
                            volumeRepository.findById(volumeMount.getVolume().getId()).orElse(null),
                            volumeMount.getVolume()
                        )
                    );
                }
                if (app.getProject() != null && volumeMount.getVolume() != null) {
                    volumeMount.getVolume().setProject(app.getProject());
                }
            });
        });
        app.getAnnotations().forEach(annotation -> annotation.setApplication(app));
        this.detachVolumesForDeletedVolumeMounts(app);

        return app;
    }

    private Volume mergeVolume(Volume destination, Volume source) {
        if (destination == null) {
            return null;
        }

        destination.setName(source.getName());
        destination.setSize(source.getSize());
        destination.setType(source.getType());
        destination.setPath(source.getPath());
        destination.setBucketUrl(source.getBucketUrl());
        destination.setRegion(source.getRegion());
        destination.setAccessKey(source.getAccessKey());

        return destination;
    }

    private void keepTransient(ApplicationDTO destination, ApplicationDTO source) {
        destination.setMessage(source.getMessage());
        destination.getContainers().forEach(
            container -> container.setRegistryPassword(
                source.getContainers().stream()
                    .filter(cont -> container.getImageName().equals(cont.getImageName()))
                    .findAny()
                    .map(ContainerDTO::getRegistryPassword)
                    .orElse(container.getRegistryPassword())
            )
        );
        destination.getContainers().forEach(
            container ->
                container.getVolumeMounts().stream()
                    .map(VolumeMountDTO::getVolume)
                    .filter(Objects::nonNull)
                    .forEach(volume -> volume.setAccessSecret(
                            source.getContainers().stream()
                                .map(ContainerDTO::getVolumeMounts)
                                .flatMap(Collection::stream)
                                .map(VolumeMountDTO::getVolume)
                                .filter(Objects::nonNull)
                                .filter(vol -> volume.getName().equals(vol.getName()))
                                .findAny()
                                .map(VolumeDTO::getAccessSecret)
                                .orElse(volume.getAccessSecret())
                        )
                    )
        );
    }

    @Transactional(readOnly = true)
    public List<ApplicationDTO> searchByName(String query) {
        log.debug("Request to search Applications by name containing : {}", query);
        return applicationRepository.findByNameContainingIgnoreCase(query)
            .stream()
            .map(app -> applicationMapper.toDto(app, new CycleAvoidingMappingContext()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApplicationDTO> findAllBasicInfo() {
        log.debug("Request to get all Applications basic info");
        return applicationRepository.findAll()
            .stream()
            .map(app -> {
                ApplicationDTO dto = new ApplicationDTO();
                dto.setId(app.getId());
                dto.setName(app.getName());
                return dto;
            })
            .collect(Collectors.toList());
    }

    /**
     * Get all the applications.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ApplicationDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Applications");
        return applicationRepository.findAll(pageable)
            .map(o -> applicationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the applications with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ApplicationDTO> findAllWithEagerRelationships(Pageable pageable) {
        return applicationRepository.findAllWithEagerRelationships(pageable)
            .map(o -> applicationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public List<ApplicationDTO> findUserApplications(String login) {
        return applicationRepository.findUserApplications(login).stream()
            .map(app -> applicationMapper.toDto(app, new CycleAvoidingMappingContext()))
            .toList();
    }

    @Transactional(readOnly = true)
    public boolean userHasApps(UserDTO user) {
        return applicationRepository.countUserApplications(user.getLogin()) > 0;
    }

    /**
     * Get one application by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ApplicationDTO> findOne(String id) {
        log.trace("Request to get Application : {}", id);
        return applicationRepository.findOneWithEagerRelationships(id)
            .map(o -> applicationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void delete(String id, String login, boolean keepVolumes) {
        log.debug("Request to delete Application : {}", id);

        var applicationDTO = transactionTemplate.execute(tx -> {
            Optional<Application> optApplication = applicationRepository.findById(id);
            if (optApplication.isEmpty()) {
                return null;
            }
            var application = optApplication.get();
            detachVolumesInUse(application, keepVolumes);

            application.setStatus(ApplicationStatus.DELETING);
            applicationRepository.updateApplicationStatus(application.getId(), ApplicationStatus.DELETING);
            return applicationMapper.toDto(application, new CycleAvoidingMappingContext());
        });

        if (applicationDTO == null) {
            return;
        }

        switch (applicationDTO.getMode()) {
            case APPLICATION -> deleteApplicationFlow.execute(applicationDTO);
            case MODEL -> deleteModelFlow.execute(applicationDTO);
        }

        transactionTemplate.executeWithoutResult(tx -> applicationRepository.deleteById(id));

        notifyUser(applicationDTO, login, MessageDTO.MessageType.DELETE);
        notificationService.notifyUser(applicationDTO, login, ProcessEvent.DELETED, null, false);
    }

    private void detachVolumesInUse(Application application, boolean keepVolumes) {
        application.getContainers().forEach(container ->
            resourceService.detachVolumesInUse(container.getVolumeMounts(), keepVolumes));
    }

    private void detachVolumesForDeletedVolumeMounts(Application app) {
        if (StringUtils.isEmpty(app.getId())) {
            return;
        }
        var remainingVolumeMounts = app.getContainers().stream()
            .map(Container::getVolumeMounts)
            .flatMap(Collection::stream)
            .toList();
        applicationRepository.findById(app.getId()).ifPresent(
            application -> application.getContainers().forEach(
                container -> container.getVolumeMounts().stream()
                    .filter(volumeMount -> !remainingVolumeMounts.contains(volumeMount))
                    .forEach(volumeMount -> volumeMountRepository.save(volumeMount.detachVolume()))
            )
        );
    }

    public void updateStatus(String id, ApplicationStatus status) {
        transactionTemplate.executeWithoutResult(tx -> applicationRepository.updateApplicationStatus(id, status));
    }

    /**
     * Save an application.
     *
     * @param applicationDTO the entity to save.
     * @return the persisted entity.
     */
    @Transactional
    public ApplicationDTO saveAdmin(ApplicationDTO applicationDTO) {
        log.debug("Request to save Application as admin : {}", applicationDTO);
        Application application = applicationMapper.toEntity(applicationDTO, new CycleAvoidingMappingContext());
        application = applicationRepository.save(application);
        return applicationMapper.toDto(application, new CycleAvoidingMappingContext());
    }

    public void notifyUser(@Nullable ApplicationDTO application, String login, MessageDTO.MessageType type) {
        log.debug("Notifying user {} of application {} update type {}", login,
            Objects.requireNonNull(application).getName(), type);
        messagingTemplate.convertAndSend("/topic/message/" + login,
            MessageDTO.builder()
                .type(type)
                .apps(Collections.singletonList(application))
                .build()
        );
    }
}
