package net.statemesh.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.statemesh.domain.ProjectAccess;
import net.statemesh.repository.ProjectAccessRepository;
import net.statemesh.service.dto.ProjectAccessDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.ProjectAccessMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link net.statemesh.domain.ProjectAccess}.
 */
@Service
@Transactional
public class ProjectAccessService {
    private final Logger log = LoggerFactory.getLogger(ProjectAccessService.class);

    private final ProjectAccessRepository projectAccessRepository;
    private final ProjectAccessMapper projectAccessMapper;

    public ProjectAccessService(ProjectAccessRepository projectAccessRepository, ProjectAccessMapper projectAccessMapper) {
        this.projectAccessRepository = projectAccessRepository;
        this.projectAccessMapper = projectAccessMapper;
    }

    /**
     * Save a projectAccess.
     *
     * @param projectAccessDTO the entity to save.
     * @return the persisted entity.
     */
    public ProjectAccessDTO save(ProjectAccessDTO projectAccessDTO) {
        log.debug("Request to save ProjectAccess : {}", projectAccessDTO);
        ProjectAccess projectAccess = projectAccessMapper.toEntity(projectAccessDTO, new CycleAvoidingMappingContext());
        projectAccess = projectAccessRepository.save(projectAccess);
        return projectAccessMapper.toDto(projectAccess, new CycleAvoidingMappingContext());
    }

    /**
     * Update a projectAccess.
     *
     * @param projectAccessDTO the entity to save.
     * @return the persisted entity.
     */
    public ProjectAccessDTO update(ProjectAccessDTO projectAccessDTO) {
        log.debug("Request to update ProjectAccess : {}", projectAccessDTO);
        ProjectAccess projectAccess = projectAccessMapper.toEntity(projectAccessDTO, new CycleAvoidingMappingContext());
        projectAccess = projectAccessRepository.save(projectAccess);
        return projectAccessMapper.toDto(projectAccess, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a projectAccess.
     *
     * @param projectAccessDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ProjectAccessDTO> partialUpdate(ProjectAccessDTO projectAccessDTO) {
        log.debug("Request to partially update ProjectAccess : {}", projectAccessDTO);

        return projectAccessRepository
            .findById(projectAccessDTO.getId())
            .map(existingProjectAccess -> {
                projectAccessMapper.partialUpdate(existingProjectAccess, projectAccessDTO);

                return existingProjectAccess;
            })
            .map(projectAccessRepository::save)
            .map(o -> projectAccessMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the projectAccesses.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ProjectAccessDTO> findAll() {
        log.debug("Request to get all ProjectAccesses");
        return projectAccessRepository.findAll().stream().map(o -> projectAccessMapper.toDto(o, new CycleAvoidingMappingContext())).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the projectAccesses with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ProjectAccessDTO> findAllWithEagerRelationships(Pageable pageable) {
        return projectAccessRepository.findAllWithEagerRelationships(pageable).map(o -> projectAccessMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get one projectAccess by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ProjectAccessDTO> findOne(String id) {
        log.debug("Request to get ProjectAccess : {}", id);
        return projectAccessRepository.findOneWithEagerRelationships(id).map(o -> projectAccessMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the projectAccess by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete ProjectAccess : {}", id);
        projectAccessRepository.deleteById(id);
    }
}
