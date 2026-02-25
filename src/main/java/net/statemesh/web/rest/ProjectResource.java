package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Projects", description = "Project management and lifecycle")
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

    @Operation(summary = "Create a new project")
    @ApiResponse(responseCode = "201", description = "Project created")
    @ApiResponse(responseCode = "400", description = "Project already has an ID")
    @PostMapping("")
    public ResponseEntity<ProjectDTO> createProject(@Valid @RequestBody ProjectDTO projectDTO,
                                                    Principal principal) throws URISyntaxException {
        log.debug("REST request to save Project : {}", projectDTO);
        if (projectDTO.getId() != null) {
            throw new BadRequestAlertException("A new project cannot already have an ID", ENTITY_NAME, "idexists");
        }

        projectDTO
            .datacenterName(
                applicationProperties.getProfile().getDatacenters().parallelStream().findAny().orElse(null)
            )
            .rayCluster(
                applicationProperties.getProfile().getRayClusters().parallelStream()
                    .map(ApplicationProperties.RayCluster::getName)
                    .findAny()
                    .orElse(null)
            )
            .setUser(userService.findOne(principal.getName()).orElse(null));

        ProjectDTO result = projectService.save(projectDTO, principal.getName());
        return ResponseEntity
            .created(new URI("/api/projects/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update an existing project")
    @ApiResponse(responseCode = "200", description = "Project updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<ProjectDTO> updateProject(
        @Parameter(description = "Project ID") @PathVariable(value = "id", required = false) final String id,
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

    @Operation(summary = "Partially update a project")
    @ApiResponse(responseCode = "200", description = "Project partially updated")
    @ApiResponse(responseCode = "404", description = "Not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ProjectDTO> partialUpdateProject(
        @Parameter(description = "Project ID") @PathVariable(value = "id", required = false) final String id,
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

    @Operation(summary = "Search projects by name")
    @ApiResponse(responseCode = "200", description = "Projects returned")
    @GetMapping("/search")
    public ResponseEntity<List<ProjectDTO>> searchByName(
        @Parameter(description = "Search query") @RequestParam("query") String query) {
        return ResponseEntity.ok(projectService.searchByName(query));
    }

    @Operation(summary = "Get all projects basic info")
    @ApiResponse(responseCode = "200", description = "Basic info returned")
    @GetMapping("/basic")
    public ResponseEntity<List<ProjectDTO>> getBasicInfo() {
        return ResponseEntity.ok(projectService.findAllBasicInfo());
    }

    @Operation(summary = "Query projects with criteria (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated projects returned")
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

    @Operation(summary = "Get a project by ID")
    @ApiResponse(responseCode = "200", description = "Project found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProject(
        @Parameter(description = "Project ID") @PathVariable String id) {
        log.debug("REST request to get Project : {}", id);
        Optional<ProjectDTO> projectDTO = projectService.findOne(id);
        return ResponseUtil.wrapOrNotFound(projectDTO);
    }

    @Operation(summary = "Get project resources")
    @ApiResponse(responseCode = "200", description = "Project resources returned")
    @GetMapping("/{id}/resources")
    public ResponseEntity<List<ProjectResourceDTO>> resources(
        @Parameter(description = "Project ID") @PathVariable(name = "id") String id) {
        return ResponseEntity.ok(projectService.getResources(id));
    }

    @Operation(summary = "Delete a project")
    @ApiResponse(responseCode = "204", description = "Project deleted")
    @ApiResponse(responseCode = "409", description = "Project has applications")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProject(
        @Parameter(description = "Project ID") @PathVariable(name = "id") String id, Principal principal) {
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
        } catch (DataIntegrityViolationException e) {
            throw new ProjectHasApplicationsException();
        }
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @Operation(summary = "Delete multiple projects")
    @ApiResponse(responseCode = "204", description = "Projects deleted")
    @ApiResponse(responseCode = "409", description = "One or more projects have applications")
    @PostMapping("/del")
    public ResponseEntity<Void> deleteProjects(@RequestBody IdCollection idCollection) {
        log.debug("REST request to delete Projects : {}", idCollection.getIds());
        try {
            projectService.deleteAll(idCollection.getIds());
        } catch (DataIntegrityViolationException e) {
            throw new ProjectHasApplicationsException();
        }
        return ResponseEntity.noContent().build();
    }
}
