package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import net.statemesh.repository.ZoneRepository;
import net.statemesh.service.ZoneService;
import net.statemesh.service.dto.ZoneDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/zones")
@Tag(name = "Zone", description = "Zone management")
public class ZoneResource {
    private final Logger log = LoggerFactory.getLogger(ZoneResource.class);

    private static final String ENTITY_NAME = "zone";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ZoneService zoneService;
    private final ZoneRepository zoneRepository;

    public ZoneResource(ZoneService zoneService, ZoneRepository zoneRepository) {
        this.zoneService = zoneService;
        this.zoneRepository = zoneRepository;
    }

    @Operation(summary = "Create a new zone")
    @ApiResponse(responseCode = "201", description = "Zone created")
    @ApiResponse(responseCode = "400", description = "Invalid input or ID already exists")
    @PostMapping("")
    public ResponseEntity<ZoneDTO> createZone(@Valid @RequestBody ZoneDTO zoneDTO) throws URISyntaxException {
        log.debug("REST request to save Zone : {}", zoneDTO);
        if (zoneDTO.getId() != null) {
            throw new BadRequestAlertException("A new zone cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ZoneDTO result = zoneService.save(zoneDTO);
        return ResponseEntity
            .created(new URI("/api/zones/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update an existing zone")
    @ApiResponse(responseCode = "200", description = "Zone updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
    @PutMapping("/{id}")
    public ResponseEntity<ZoneDTO> updateZone(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody ZoneDTO zoneDTO) {
        log.debug("REST request to update Zone : {}, {}", id, zoneDTO);
        if (!Objects.equals(id, zoneDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!zoneRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ZoneDTO result = zoneService.update(zoneDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, zoneDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update a zone")
    @ApiResponse(responseCode = "200", description = "Zone partially updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
    @ApiResponse(responseCode = "404", description = "Zone not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ZoneDTO> partialUpdateZone(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody ZoneDTO zoneDTO) {
        log.debug("REST request to partial update Zone partially : {}, {}", id, zoneDTO);
        if (!Objects.equals(id, zoneDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!zoneRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ZoneDTO> result = zoneService.partialUpdate(zoneDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, zoneDTO.getId())
        );
    }

    @Operation(summary = "Get all zones")
    @ApiResponse(responseCode = "200", description = "List of zones returned")
    @GetMapping("")
    public List<ZoneDTO> getAllZones() {
        log.trace("REST request to get all Zones");
        return zoneService.findAll();
    }

    @Operation(summary = "Get a zone by ID")
    @ApiResponse(responseCode = "200", description = "Zone returned")
    @ApiResponse(responseCode = "404", description = "Zone not found")
    @GetMapping("/{id}")
    public ResponseEntity<ZoneDTO> getZone(@PathVariable String id) {
        log.debug("REST request to get Zone : {}", id);
        Optional<ZoneDTO> zoneDTO = zoneService.findOne(id);
        return ResponseUtil.wrapOrNotFound(zoneDTO);
    }

    @Operation(summary = "Delete a zone")
    @ApiResponse(responseCode = "204", description = "Zone deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable String id) {
        log.debug("REST request to delete Zone : {}", id);
        zoneService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
