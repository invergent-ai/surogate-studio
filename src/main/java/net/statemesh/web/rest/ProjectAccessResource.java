package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/project-accesses")
@Tag(name = "Project Access", description = "Project access control management")
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

    @Operation(summary = "Create a project access entry")
    @ApiResponse(responseCode = "201", description = "Project access created")
    @ApiResponse(responseCode = "400", description = "Already has an ID")
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

    @Operation(summary = "Update a project access entry")
    @ApiResponse(responseCode = "200", description = "Project access updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<ProjectAccessDTO> updateProjectAccess(
        @Parameter(description = "Project access ID") @PathVariable(value = "id", required = false) final String id,
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

    @Operation(summary = "Partially update a project access entry")
    @ApiResponse(responseCode = "200", description = "Project access partially updated")
    @ApiResponse(responseCode = "404", description = "Not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ProjectAccessDTO> partialUpdateProjectAccess(
        @Parameter(description = "Project access ID") @PathVariable(value = "id", required = false) final String id,
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

    @Operation(summary = "Get all project access entries")
    @ApiResponse(responseCode = "200", description = "List of project access entries")
    @GetMapping("")
    public List<ProjectAccessDTO> getAllProjectAccesses() {
        log.debug("REST request to get all ProjectAccesses");
        return projectAccessService.findAll();
    }

    @Operation(summary = "Get a project access entry by ID")
    @ApiResponse(responseCode = "200", description = "Project access found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectAccessDTO> getProjectAccess(
        @Parameter(description = "Project access ID") @PathVariable String id) {
        log.debug("REST request to get ProjectAccess : {}", id);
        Optional<ProjectAccessDTO> projectAccessDTO = projectAccessService.findOne(id);
        return ResponseUtil.wrapOrNotFound(projectAccessDTO);
    }

    @Operation(summary = "Delete a project access entry")
    @ApiResponse(responseCode = "204", description = "Project access deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProjectAccess(
        @Parameter(description = "Project access ID") @PathVariable String id) {
        log.debug("REST request to delete ProjectAccess : {}", id);
        projectAccessService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
