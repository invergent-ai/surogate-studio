package net.statemesh.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.repository.ProjectRepository;
import net.statemesh.service.ProjectService;
import net.statemesh.service.UserService;
import net.statemesh.service.criteria.ProjectCriteria;
import net.statemesh.service.dto.MessageDTO;
import net.statemesh.service.dto.ProjectDTO;
import net.statemesh.service.dto.ProjectResourceDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import net.statemesh.service.exception.ProjectHasApplicationsException;
import net.statemesh.service.query.ProjectQueryService;
import net.statemesh.web.rest.vm.IdCollection;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * REST controller for managing {@link net.statemesh.domain.Project}.
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
public class ProjectResource {
    private static final String ENTITY_NAME = "project";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProjectService projectService;
    private final ProjectQueryService projectQueryService;
    private final UserService userService;
    private final ProjectRepository projectRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationProperties applicationProperties;

    /**
     * {@code POST  /projects} : Create a new project.
     *
     * @param projectDTO the projectDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new projectDTO, or with status {@code 400 (Bad Request)} if the project has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectDTO projectDTO,
                                                    Principal principal) throws URISyntaxException {
        log.debug("REST request to save Project : {}", projectDTO);
        if (projectDTO.getId() != null) {
            throw new BadRequestAlertException("A new project cannot already have an ID", ENTITY_NAME, "idexists");
        }

        projectDTO
            .datacenterName(
                // We could implement a datacenter selection strategy
                applicationProperties.getProfile().getDatacenters().parallelStream().findAny().orElse(null)
            )
            .rayCluster(
                // We could implement a ray cluster selection strategy
                applicationProperties.getProfile().getRayClusters().parallelStream()
                    .map(ApplicationProperties.RayCluster::getName)
                    .findAny()
                    .orElse(null)
            )
            .setUser(
                userService.findOne(principal.getName()).orElse(null)
            );

        ProjectDTO result = projectService.save(projectDTO, principal.getName());

        return ResponseEntity
            .created(new URI("/api/projects/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /projects/:id} : Updates an existing project.
     *
     * @param id the id of the projectDTO to save.
     * @param projectDTO the projectDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated projectDTO,
     * or with status {@code 400 (Bad Request)} if the projectDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the projectDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody ProjectDTO projectDTO) {
        log.debug("REST request to update Project : {}, {}", id, projectDTO);
        if (!Objects.equals(id, projectDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!projectRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ProjectDTO result = projectService.update(projectDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, projectDTO.getId()))
            .body(result);
    }

    /**
     * {@code PATCH  /projects/:id} : Partial updates given fields of an existing project, field will ignore if it is null
     *
     * @param id the id of the projectDTO to save.
     * @param projectDTO the projectDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated projectDTO,
     * or with status {@code 400 (Bad Request)} if the projectDTO is not valid,
     * or with status {@code 404 (Not Found)} if the projectDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the projectDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ProjectDTO> partialUpdateProject(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody ProjectDTO projectDTO) {
        log.debug("REST request to partial update Project partially : {}, {}", id, projectDTO);
        if (!Objects.equals(id, projectDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!projectRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ProjectDTO> result = projectService.partialUpdate(projectDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, projectDTO.getId())
        );
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProjectDTO>> searchByName(@RequestParam("query") String query) {
        return ResponseEntity.ok(projectService.searchByName(query));
    }

    @GetMapping("/basic")
    public ResponseEntity<List<ProjectDTO>> getBasicInfo() {
        return ResponseEntity.ok(projectService.findAllBasicInfo());
    }

    /**
     * {@code GET  /projects} : get all the projects.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of projects in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ProjectDTO>> getAllProjects(
        ProjectCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        Principal principal
    ) {
        log.trace("REST request to get a page of Projects");
        Page<ProjectDTO> page = projectQueryService.findByCriteria(criteria, pageable, principal.getName());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /projects/:id} : get the "id" project.
     *
     * @param id the id of the projectDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the projectDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProject(@PathVariable String id) {
        log.debug("REST request to get Project : {}", id);
        Optional<ProjectDTO> projectDTO = projectService.findOne(id);
        return ResponseUtil.wrapOrNotFound(projectDTO);
    }


    @GetMapping("/{id}/resources")
    public ResponseEntity<List<ProjectResourceDTO>> resources(@PathVariable(name = "id") String id) {
        return ResponseEntity.ok(projectService.getResources(id));
    }

    /**
     * {@code DELETE  /projects/:id} : delete the "id" project.
     *
     * @param id the id of the projectDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(@PathVariable(name = "id") String id, Principal principal) {
        log.debug("REST request to delete Project : {}", id);
        try {
            projectService.findOne(id).ifPresent(projectDTO -> {
                projectService.delete(id);
                messagingTemplate.convertAndSend("/topic/message/" + principal.getName(),
                    MessageDTO.builder()
                        .type(MessageDTO.MessageType.DELETE)
                        .projects(Collections.singletonList(projectDTO))
                        .build()
                );
            });
        } catch(DataIntegrityViolationException e) {
            throw new ProjectHasApplicationsException();
        }
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @PostMapping("/del")
    public ResponseEntity<Void> deleteProjects(@RequestBody IdCollection idCollection) {
        log.debug("REST request to delete Projects : {}", idCollection.getIds());
        try {
            projectService.deleteAll(idCollection.getIds());
        } catch(DataIntegrityViolationException e) {
            throw new ProjectHasApplicationsException();
        }
        return ResponseEntity
            .noContent()
            .build();
    }
}
