package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import net.statemesh.repository.EnvironmentVariableRepository;
import net.statemesh.service.EnvironmentVariableService;
import net.statemesh.service.dto.EnvironmentVariableDTO;
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
@RequestMapping("/api/environment-variables")
@RequiredArgsConstructor
@Tag(name = "Environment Variables", description = "Environment variable management")
public class EnvironmentVariableResource {
    private final Logger log = LoggerFactory.getLogger(EnvironmentVariableResource.class);
    private static final String ENTITY_NAME = "environmentVariable";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final EnvironmentVariableService environmentVariableService;
    private final EnvironmentVariableRepository environmentVariableRepository;

    @Operation(summary = "Create an environment variable")
    @ApiResponse(responseCode = "201", description = "Environment variable created")
    @ApiResponse(responseCode = "400", description = "Already has an ID")
    @PostMapping("")
    public ResponseEntity<EnvironmentVariableDTO> createEnvironmentVariable(
        @Valid @RequestBody EnvironmentVariableDTO environmentVariableDTO
    ) throws URISyntaxException {
        log.debug("REST request to save EnvironmentVariable : {}", environmentVariableDTO);
        if (environmentVariableDTO.getId() != null) {
            throw new BadRequestAlertException("A new environmentVariable cannot already have an ID", ENTITY_NAME, "idexists");
        }
        EnvironmentVariableDTO result = environmentVariableService.save(environmentVariableDTO);
        return ResponseEntity
            .created(new URI("/api/environment-variables/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update an environment variable")
    @ApiResponse(responseCode = "200", description = "Environment variable updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<EnvironmentVariableDTO> updateEnvironmentVariable(
        @Parameter(description = "Environment variable ID") @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody EnvironmentVariableDTO environmentVariableDTO) {
        log.debug("REST request to update EnvironmentVariable : {}, {}", id, environmentVariableDTO);
        if (!Objects.equals(id, environmentVariableDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!environmentVariableRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        EnvironmentVariableDTO result = environmentVariableService.update(environmentVariableDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, environmentVariableDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update an environment variable")
    @ApiResponse(responseCode = "200", description = "Environment variable partially updated")
    @ApiResponse(responseCode = "404", description = "Not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<EnvironmentVariableDTO> partialUpdateEnvironmentVariable(
        @Parameter(description = "Environment variable ID") @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody EnvironmentVariableDTO environmentVariableDTO) {
        log.debug("REST request to partial update EnvironmentVariable partially : {}, {}", id, environmentVariableDTO);
        if (!Objects.equals(id, environmentVariableDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!environmentVariableRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<EnvironmentVariableDTO> result = environmentVariableService.partialUpdate(environmentVariableDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, environmentVariableDTO.getId())
        );
    }

    @Operation(summary = "Get all environment variables")
    @ApiResponse(responseCode = "200", description = "List of environment variables")
    @GetMapping("")
    public List<EnvironmentVariableDTO> getAllEnvironmentVariables() {
        log.debug("REST request to get all EnvironmentVariables");
        return environmentVariableService.findAll();
    }

    @Operation(summary = "Get an environment variable by ID")
    @ApiResponse(responseCode = "200", description = "Environment variable found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentVariableDTO> getEnvironmentVariable(
        @Parameter(description = "Environment variable ID") @PathVariable String id) {
        log.debug("REST request to get EnvironmentVariable : {}", id);
        Optional<EnvironmentVariableDTO> environmentVariableDTO = environmentVariableService.findOne(id);
        return ResponseUtil.wrapOrNotFound(environmentVariableDTO);
    }

    @Operation(summary = "Delete an environment variable")
    @ApiResponse(responseCode = "204", description = "Environment variable deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnvironmentVariable(
        @Parameter(description = "Environment variable ID") @PathVariable String id) {
        log.debug("REST request to delete EnvironmentVariable : {}", id);
        environmentVariableService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
