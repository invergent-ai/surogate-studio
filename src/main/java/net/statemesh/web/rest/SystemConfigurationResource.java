package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.statemesh.repository.SystemConfigurationRepository;
import net.statemesh.service.SystemConfigurationService;
import net.statemesh.service.dto.SystemConfigurationDTO;
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
@RequestMapping("/api/system-configurations")
@Tag(name = "System Configuration", description = "System configuration management")
public class SystemConfigurationResource {
    private final Logger log = LoggerFactory.getLogger(SystemConfigurationResource.class);

    private static final String ENTITY_NAME = "systemConfiguration";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final SystemConfigurationService systemConfigurationService;

    private final SystemConfigurationRepository systemConfigurationRepository;

    public SystemConfigurationResource(
        SystemConfigurationService systemConfigurationService,
        SystemConfigurationRepository systemConfigurationRepository
    ) {
        this.systemConfigurationService = systemConfigurationService;
        this.systemConfigurationRepository = systemConfigurationRepository;
    }

    @Operation(summary = "Create a new system configuration")
    @ApiResponse(responseCode = "201", description = "System configuration created")
    @ApiResponse(responseCode = "400", description = "Invalid input or ID already exists")
    @PostMapping("")
    public ResponseEntity<SystemConfigurationDTO> createSystemConfiguration(@RequestBody SystemConfigurationDTO systemConfigurationDTO)
        throws URISyntaxException {
        log.debug("REST request to save SystemConfiguration : {}", systemConfigurationDTO);
        if (systemConfigurationDTO.getId() != null) {
            throw new BadRequestAlertException("A new systemConfiguration cannot already have an ID", ENTITY_NAME, "idexists");
        }
        SystemConfigurationDTO result = systemConfigurationService.save(systemConfigurationDTO);
        return ResponseEntity
            .created(new URI("/api/system-configurations/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update an existing system configuration")
    @ApiResponse(responseCode = "200", description = "System configuration updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
    @PutMapping("/{id}")
    public ResponseEntity<SystemConfigurationDTO> updateSystemConfiguration(
        @PathVariable(value = "id", required = false) final String id,
        @RequestBody SystemConfigurationDTO systemConfigurationDTO) {
        log.debug("REST request to update SystemConfiguration : {}, {}", id, systemConfigurationDTO);
        if (!Objects.equals(id, systemConfigurationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!systemConfigurationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        SystemConfigurationDTO result = systemConfigurationService.update(systemConfigurationDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, systemConfigurationDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update a system configuration")
    @ApiResponse(responseCode = "200", description = "System configuration partially updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
    @ApiResponse(responseCode = "404", description = "System configuration not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<SystemConfigurationDTO> partialUpdateSystemConfiguration(
        @PathVariable(value = "id", required = false) final String id,
        @RequestBody SystemConfigurationDTO systemConfigurationDTO
    ) {
        log.debug("REST request to partial update SystemConfiguration partially : {}, {}", id, systemConfigurationDTO);
        if (!Objects.equals(id, systemConfigurationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!systemConfigurationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<SystemConfigurationDTO> result = systemConfigurationService.partialUpdate(systemConfigurationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, systemConfigurationDTO.getId())
        );
    }

    @Operation(summary = "Get all system configurations")
    @ApiResponse(responseCode = "200", description = "List of system configurations returned")
    @GetMapping("")
    public List<SystemConfigurationDTO> getAllSystemConfigurations() {
        log.debug("REST request to get all SystemConfigurations");
        return systemConfigurationService.findAll();
    }

    @Operation(summary = "Get a system configuration by ID")
    @ApiResponse(responseCode = "200", description = "System configuration returned")
    @ApiResponse(responseCode = "404", description = "System configuration not found")
    @GetMapping("/{id}")
    public ResponseEntity<SystemConfigurationDTO> getSystemConfiguration(@PathVariable String id) {
        log.debug("REST request to get SystemConfiguration : {}", id);
        Optional<SystemConfigurationDTO> systemConfigurationDTO = systemConfigurationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(systemConfigurationDTO);
    }

    @Operation(summary = "Delete a system configuration")
    @ApiResponse(responseCode = "204", description = "System configuration deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSystemConfiguration(@PathVariable String id) {
        log.debug("REST request to delete SystemConfiguration : {}", id);
        systemConfigurationService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
