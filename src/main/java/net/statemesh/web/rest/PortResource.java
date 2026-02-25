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
import net.statemesh.repository.PortRepository;
import net.statemesh.service.PortService;
import net.statemesh.service.dto.PortDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api/ports")
@Tag(name = "Ports", description = "Port configuration management")
public class PortResource {
    private final Logger log = LoggerFactory.getLogger(PortResource.class);
    private static final String ENTITY_NAME = "port";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final PortService portService;
    private final PortRepository portRepository;

    public PortResource(PortService portService, PortRepository portRepository) {
        this.portService = portService;
        this.portRepository = portRepository;
    }

    @Operation(summary = "Create a port")
    @ApiResponse(responseCode = "201", description = "Port created")
    @ApiResponse(responseCode = "400", description = "Already has an ID")
    @PostMapping("")
    public ResponseEntity<PortDTO> createPort(@Valid @RequestBody PortDTO portDTO) throws URISyntaxException {
        log.debug("REST request to save Port : {}", portDTO);
        if (portDTO.getId() != null) {
            throw new BadRequestAlertException("A new port cannot already have an ID", ENTITY_NAME, "idexists");
        }
        PortDTO result = portService.save(portDTO);
        return ResponseEntity
            .created(new URI("/api/ports/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update a port")
    @ApiResponse(responseCode = "200", description = "Port updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<PortDTO> updatePort(
        @Parameter(description = "Port ID") @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody PortDTO portDTO) {
        log.debug("REST request to update Port : {}, {}", id, portDTO);
        if (!Objects.equals(id, portDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!portRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        PortDTO result = portService.update(portDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, portDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update a port")
    @ApiResponse(responseCode = "200", description = "Port partially updated")
    @ApiResponse(responseCode = "404", description = "Not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<PortDTO> partialUpdatePort(
        @Parameter(description = "Port ID") @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody PortDTO portDTO) {
        log.debug("REST request to partial update Port partially : {}, {}", id, portDTO);
        if (!Objects.equals(id, portDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!portRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<PortDTO> result = portService.partialUpdate(portDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, portDTO.getId())
        );
    }

    @Operation(summary = "Get all ports")
    @ApiResponse(responseCode = "200", description = "List of ports")
    @GetMapping("")
    public List<PortDTO> getAllPorts() {
        log.debug("REST request to get all Ports");
        return portService.findAll();
    }

    @Operation(summary = "Get a port by ID")
    @ApiResponse(responseCode = "200", description = "Port found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public ResponseEntity<PortDTO> getPort(
        @Parameter(description = "Port ID") @PathVariable String id) {
        log.debug("REST request to get Port : {}", id);
        Optional<PortDTO> portDTO = portService.findOne(id);
        return ResponseUtil.wrapOrNotFound(portDTO);
    }

    @Operation(summary = "Delete a port")
    @ApiResponse(responseCode = "204", description = "Port deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePort(
        @Parameter(description = "Port ID") @PathVariable String id) {
        log.debug("REST request to delete Port : {}", id);
        portService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
