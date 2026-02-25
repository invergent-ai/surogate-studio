package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import net.statemesh.repository.VolumeMountRepository;
import net.statemesh.service.VolumeMountService;
import net.statemesh.service.criteria.VolumeMountCriteria;
import net.statemesh.service.dto.VolumeMountDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import net.statemesh.service.query.VolumeMountQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/volume-mounts")
@Tag(name = "Volume Mount", description = "Volume mount management")
public class VolumeMountResource {
    private final Logger log = LoggerFactory.getLogger(VolumeMountResource.class);

    private static final String ENTITY_NAME = "volumeMount";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final VolumeMountService volumeMountService;
    private final VolumeMountRepository volumeMountRepository;
    private final VolumeMountQueryService volumeMountQueryService;

    public VolumeMountResource(
        VolumeMountService volumeMountService,
        VolumeMountRepository volumeMountRepository,
        VolumeMountQueryService volumeMountQueryService
    ) {
        this.volumeMountService = volumeMountService;
        this.volumeMountRepository = volumeMountRepository;
        this.volumeMountQueryService = volumeMountQueryService;
    }

    @Operation(summary = "Create a new volume mount")
    @ApiResponse(responseCode = "201", description = "Volume mount created")
    @ApiResponse(responseCode = "400", description = "Invalid input or ID already exists")
    @PostMapping("")
    public ResponseEntity<VolumeMountDTO> createVolumeMount(@Valid @RequestBody VolumeMountDTO volumeMountDTO, Principal principal) throws URISyntaxException {
        log.debug("REST request to save VolumeMount : {}", volumeMountDTO);
        if (volumeMountDTO.getId() != null) {
            throw new BadRequestAlertException("A new volumeMount cannot already have an ID", ENTITY_NAME, "idexists");
        }
        VolumeMountDTO result = volumeMountService.save(volumeMountDTO);
        return ResponseEntity
            .created(new URI("/api/volume-mounts/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update an existing volume mount")
    @ApiResponse(responseCode = "200", description = "Volume mount updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
    @PutMapping("/{id}")
    public ResponseEntity<VolumeMountDTO> updateVolumeMount(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody VolumeMountDTO volumeMountDTO) {
        log.debug("REST request to update VolumeMount : {}, {}", id, volumeMountDTO);
        if (!Objects.equals(id, volumeMountDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!volumeMountRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        VolumeMountDTO result = volumeMountService.update(volumeMountDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, volumeMountDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update a volume mount")
    @ApiResponse(responseCode = "200", description = "Volume mount partially updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
    @ApiResponse(responseCode = "404", description = "Volume mount not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<VolumeMountDTO> partialUpdateVolumeMount(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody VolumeMountDTO volumeMountDTO) {
        log.debug("REST request to partial update VolumeMount partially : {}, {}", id, volumeMountDTO);
        if (!Objects.equals(id, volumeMountDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!volumeMountRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<VolumeMountDTO> result = volumeMountService.partialUpdate(volumeMountDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, volumeMountDTO.getId())
        );
    }

    @Operation(summary = "Query volume mounts by criteria")
    @ApiResponse(responseCode = "200", description = "List of volume mounts returned")
    @GetMapping("")
    public ResponseEntity<List<VolumeMountDTO>> query(
        VolumeMountCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        Principal principal
    ) {
        Page<VolumeMountDTO> resultPage = volumeMountQueryService.findByCriteria(criteria, pageable, principal.getName());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), resultPage);
        return ResponseEntity.ok().headers(headers).body(resultPage.getContent());
    }

    @Operation(summary = "Get a volume mount by ID")
    @ApiResponse(responseCode = "200", description = "Volume mount returned")
    @ApiResponse(responseCode = "404", description = "Volume mount not found")
    @GetMapping("/{id}")
    public ResponseEntity<VolumeMountDTO> getVolumeMount(@PathVariable String id) {
        log.debug("REST request to get VolumeMount : {}", id);
        Optional<VolumeMountDTO> volumeMountDTO = volumeMountService.findOne(id);
        return ResponseUtil.wrapOrNotFound(volumeMountDTO);
    }

    @Operation(summary = "Delete a volume mount")
    @ApiResponse(responseCode = "204", description = "Volume mount deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVolumeMount(@PathVariable String id, Principal principal) {
        log.debug("REST request to delete VolumeMount : {}", id);
        volumeMountService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
