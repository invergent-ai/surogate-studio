package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import net.statemesh.domain.Project;
import net.statemesh.domain.enumeration.VolumeType;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.repository.ProjectRepository;
import net.statemesh.repository.ZoneRepository;
import net.statemesh.service.dto.ProjectDTO;
import net.statemesh.service.dto.ProjectResourceDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.ProjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Project}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ProjectService {
    private final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;
    private final OrganizationService organizationService;
    private final ZoneRepository zoneRepository;

    /**
     * Save a project.
     *
     * @param projectDTO the entity to save.
     * @return the persisted entity.
     */
    @Transactional
    public ProjectDTO save(ProjectDTO projectDTO, String login) {
        log.debug("Request to save Project : {}", projectDTO);
        Project project = projectMapper.toEntity(
            projectDTO
                .organization(organizationService.defaultOrganization()),
            new CycleAvoidingMappingContext()
        );
        project = projectRepository.save(project);
        project.setNamespace(generateNamespace(project, login));
        return projectMapper.toDto(project, new CycleAvoidingMappingContext());
    }

    /**
     * Update a project.
     *
     * @param projectDTO the entity to save.
     * @return the persisted entity.
     */
    public ProjectDTO update(ProjectDTO projectDTO) {
        log.debug("Request to update Project : {}", projectDTO);
        Project project = projectMapper.toEntity(projectDTO, new CycleAvoidingMappingContext());
        project = projectRepository.save(project);
        return projectMapper.toDto(project, new CycleAvoidingMappingContext());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> searchByName(String query) {
        log.debug("Request to search Projects by name containing : {}", query);
        List<Project> projects = projectRepository.findByNameContainingIgnoreCase(query);
        return projectMapper.toDto(projects, new CycleAvoidingMappingContext());
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> findAllBasicInfo() {
        log.debug("Request to get all Projects basic info");
        return projectRepository.findAll()
            .stream()
            .map(ProjectDTO::basicProjection)
            .collect(Collectors.toList());
    }

    /**
     * Partially update a project.
     *
     * @param projectDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ProjectDTO> partialUpdate(ProjectDTO projectDTO) {
        log.debug("Request to partially update Project : {}", projectDTO);

        return projectRepository
            .findById(projectDTO.getId())
            .map(existingProject -> {
                if (existingProject.getZone() != null && projectDTO.getZone() != null &&
                    !existingProject.getZone().getId().equals(projectDTO.getZone().getId())) {
                    zoneRepository.findById(projectDTO.getZone().getId()).ifPresent(existingProject::setZone);
                    projectDTO.setZone(null);
                    existingProject.setCluster(null);
                }
                projectMapper.partialUpdate(existingProject, projectDTO);

                return existingProject;
            })
            .map(projectRepository::save)
            .map(o -> projectMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the projects.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ProjectDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Projects");
        return projectRepository.findAll(pageable).map(o -> projectMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the projects with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ProjectDTO> findAllWithEagerRelationships(Pageable pageable) {
        return projectRepository.findAllWithEagerRelationships(pageable).map(o -> projectMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get one project by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ProjectDTO> findOne(String id) {
        return projectRepository.findOneWithEagerRelationships(id)
            .map(o -> projectMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the project by id.
     *
     * @param id the id of the entity.
     */
    @Transactional
    public void delete(String id) {
        log.debug("Request to delete Project : {}", id);
        projectRepository.findById(id).ifPresent(Project::delete);
    }

    @Transactional
    public void deleteAll(List<String> ids) {
        log.debug("Request to delete a list of Projects : {}", ids);
        projectRepository.findAllById(ids).forEach(Project::delete);
    }

    @Transactional(readOnly = true)
    public List<ProjectDTO> getByUser(String login) {
        return projectRepository.findByUser_Login(login).stream()
            .map(project -> projectMapper.toDto(project, new CycleAvoidingMappingContext()))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProjectResourceDTO> getResources(String projectId) {
        var resources = new ArrayList<ProjectResourceDTO>();
        var project = projectRepository.findById(projectId).orElseThrow();

        resources.addAll(project.getApplications().stream().map(app ->
            new ProjectResourceDTO(app.getId(), app.getName(), ProjectResourceDTO.ProjectResourceType.APPLICATION)
        ).toList());
        resources.addAll(project.getVolumes().stream()
            .filter(v -> !VolumeType.HOST_PATH.equals(v.getType()) && !VolumeType.SHM.equals(v.getType()))
            .map(vol ->
                new ProjectResourceDTO(vol.getId(), vol.getName(), ProjectResourceDTO.ProjectResourceType.VOLUME)
            ).toList());

        return resources;
    }

    private String generateNamespace(Project project, String login) {
        return NamingUtils.rfc1123Name(
            Stream.of(
                    login.replaceAll("@", "-").replaceAll("\\.", "-"),
                    project.getName(),
                    project.getId()
                )
                .map(Objects::toString)
                .collect(Collectors.joining("-"))
                .toLowerCase()
        );
    }
}
