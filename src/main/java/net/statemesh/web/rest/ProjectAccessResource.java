package net.statemesh.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.statemesh.repository.ProjectAccessRepository;
import net.statemesh.service.ProjectAccessService;
import net.statemesh.service.dto.ProjectAccessDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link net.statemesh.domain.ProjectAccess}.
 */
@RestController
@RequestMapping("/api/project-accesses")
public class ProjectAccessResource {
    private final Logger log = LoggerFactory.getLogger(ProjectAccessResource.class);

    private static final String ENTITY_NAME = "projectAccess";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProjectAccessService projectAccessService;

    private final ProjectAccessRepository projectAccessRepository;

    public ProjectAccessResource(ProjectAccessService projectAccessService, ProjectAccessRepository projectAccessRepository) {
        this.projectAccessService = projectAccessService;
        this.projectAccessRepository = projectAccessRepository;
    }

    /**
     * {@code POST  /project-accesses} : Create a new projectAccess.
     *
     * @param projectAccessDTO the projectAccessDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new projectAccessDTO, or with status {@code 400 (Bad Request)} if the projectAccess has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ProjectAccessDTO> createProjectAccess(@Valid @RequestBody ProjectAccessDTO projectAccessDTO)
        throws URISyntaxException {
        log.debug("REST request to save ProjectAccess : {}", projectAccessDTO);
        if (projectAccessDTO.getId() != null) {
            throw new BadRequestAlertException("A new projectAccess cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ProjectAccessDTO result = projectAccessService.save(projectAccessDTO);
        return ResponseEntity
            .created(new URI("/api/project-accesses/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /project-accesses/:id} : Updates an existing projectAccess.
     *
     * @param id the id of the projectAccessDTO to save.
     * @param projectAccessDTO the projectAccessDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated projectAccessDTO,
     * or with status {@code 400 (Bad Request)} if the projectAccessDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the projectAccessDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectAccessDTO> updateProjectAccess(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody ProjectAccessDTO projectAccessDTO) {
        log.debug("REST request to update ProjectAccess : {}, {}", id, projectAccessDTO);
        if (!Objects.equals(id, projectAccessDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!projectAccessRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ProjectAccessDTO result = projectAccessService.update(projectAccessDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, projectAccessDTO.getId()))
            .body(result);
    }

    /**
     * {@code PATCH  /project-accesses/:id} : Partial updates given fields of an existing projectAccess, field will ignore if it is null
     *
     * @param id the id of the projectAccessDTO to save.
     * @param projectAccessDTO the projectAccessDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated projectAccessDTO,
     * or with status {@code 400 (Bad Request)} if the projectAccessDTO is not valid,
     * or with status {@code 404 (Not Found)} if the projectAccessDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the projectAccessDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ProjectAccessDTO> partialUpdateProjectAccess(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody ProjectAccessDTO projectAccessDTO) {
        log.debug("REST request to partial update ProjectAccess partially : {}, {}", id, projectAccessDTO);
        if (!Objects.equals(id, projectAccessDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!projectAccessRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ProjectAccessDTO> result = projectAccessService.partialUpdate(projectAccessDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, projectAccessDTO.getId())
        );
    }

    /**
     * {@code GET  /project-accesses} : get all the projectAccesses.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of projectAccesses in body.
     */
    @GetMapping("")
    public List<ProjectAccessDTO> getAllProjectAccesses() {
        log.debug("REST request to get all ProjectAccesses");
        return projectAccessService.findAll();
    }

    /**
     * {@code GET  /project-accesses/:id} : get the "id" projectAccess.
     *
     * @param id the id of the projectAccessDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the projectAccessDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectAccessDTO> getProjectAccess(@PathVariable String id) {
        log.debug("REST request to get ProjectAccess : {}", id);
        Optional<ProjectAccessDTO> projectAccessDTO = projectAccessService.findOne(id);
        return ResponseUtil.wrapOrNotFound(projectAccessDTO);
    }

    /**
     * {@code DELETE  /project-accesses/:id} : delete the "id" projectAccess.
     *
     * @param id the id of the projectAccessDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProjectAccess(@PathVariable String id) {
        log.debug("REST request to delete ProjectAccess : {}", id);
        projectAccessService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
