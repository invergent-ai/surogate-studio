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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.repository.ContainerRepository;
import net.statemesh.service.query.ContainerQueryService;
import net.statemesh.service.ContainerService;
import net.statemesh.service.criteria.ContainerCriteria;
import net.statemesh.service.dto.ContainerDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api/containers")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Containers", description = "Container management for applications")
public class ContainerResource {
    private static final String ENTITY_NAME = "container";

    private final ContainerService containerService;
    private final ContainerRepository containerRepository;
    private final ContainerQueryService containerQueryService;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    @Operation(summary = "Create a new container")
    @ApiResponse(responseCode = "201", description = "Container created")
    @ApiResponse(responseCode = "400", description = "Container already has an ID")
    @PostMapping("")
    public ResponseEntity<ContainerDTO> createContainer(@Valid @RequestBody ContainerDTO containerDTO) throws URISyntaxException {
        log.debug("REST request to save Container : {}", containerDTO);
        if (containerDTO.getId() != null) {
            throw new BadRequestAlertException("A new container cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ContainerDTO result = containerService.save(containerDTO);
        return ResponseEntity
            .created(new URI("/api/containers/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update an existing container")
    @ApiResponse(responseCode = "200", description = "Container updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<ContainerDTO> updateContainer(
        @Parameter(description = "Container ID") @PathVariable(value = "id", name = "id", required = false) final String id,
        @Valid @RequestBody ContainerDTO containerDTO) {
        log.debug("REST request to update Container : {}, {}", id, containerDTO);
        if (!Objects.equals(id, containerDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!containerRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        ContainerDTO result = containerService.update(containerDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, containerDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update a container")
    @ApiResponse(responseCode = "200", description = "Container partially updated")
    @ApiResponse(responseCode = "404", description = "Container not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ContainerDTO> partialUpdateContainer(
        @Parameter(description = "Container ID") @PathVariable(value = "id", name = "id", required = false) final String id,
        @NotNull @RequestBody ContainerDTO containerDTO) {
        log.debug("REST request to partial update Container partially : {}, {}", id, containerDTO);
        if (!Objects.equals(id, containerDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!containerRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<ContainerDTO> result = containerService.partialUpdate(containerDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, containerDTO.getId())
        );
    }

    @Operation(summary = "Get all containers (unpaginated)")
    @ApiResponse(responseCode = "200", description = "List of all containers")
    @GetMapping("/all")
    public List<ContainerDTO> getAllContainers() {
        log.debug("REST request to get all Containers");
        return containerService.findAll();
    }

    @Operation(summary = "Get a container by ID")
    @ApiResponse(responseCode = "200", description = "Container found")
    @ApiResponse(responseCode = "404", description = "Container not found")
    @GetMapping("/{id}")
    public ResponseEntity<ContainerDTO> getContainer(
        @Parameter(description = "Container ID") @PathVariable(name = "id") String id) {
        log.debug("REST request to get Container : {}", id);
        Optional<ContainerDTO> containerDTO = containerService.findOne(id);
        return ResponseUtil.wrapOrNotFound(containerDTO);
    }

    @Operation(summary = "Delete a container")
    @ApiResponse(responseCode = "204", description = "Container deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContainer(
        @Parameter(description = "Container ID") @PathVariable(name = "id") String id) {
        log.debug("REST request to delete Container : {}", id);
        containerService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @Operation(summary = "Get containers by application ID")
    @ApiResponse(responseCode = "200", description = "Containers returned")
    @GetMapping("/by-application/{id}")
    public ResponseEntity<List<ContainerDTO>> getContainersByPodConfig(
        @Parameter(description = "Application ID") @PathVariable(value = "id", name = "id") String applicationId) {
        log.debug("REST request to get Containers by Application ID : {}", applicationId);
        List<ContainerDTO> containers = containerService.findAllByApplicationId(applicationId);
        return ResponseEntity.ok().body(containers);
    }

    @Operation(summary = "Query containers with criteria (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated containers returned")
    @ApiResponse(responseCode = "400", description = "Application ID is required")
    @GetMapping("")
    public ResponseEntity<List<ContainerDTO>> getAllContainers(
        ContainerCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get Containers by criteria: {}", criteria);
        if (criteria == null || criteria.getApplicationId() == null || criteria.getApplicationId().getEquals() == null) {
            throw new BadRequestAlertException("Application ID is required", ENTITY_NAME, "applicationidrequired");
        }
        Page<ContainerDTO> resultPage = containerQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), resultPage);
        return ResponseEntity.ok().headers(headers).body(resultPage.getContent());
    }
}
