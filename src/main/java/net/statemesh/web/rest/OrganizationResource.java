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
import net.statemesh.repository.OrganizationRepository;
import net.statemesh.service.OrganizationService;
import net.statemesh.service.dto.OrganizationDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organizations", description = "Organization management")
public class OrganizationResource {

    private final Logger log = LoggerFactory.getLogger(OrganizationResource.class);
    private static final String ENTITY_NAME = "organization";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final OrganizationService organizationService;
    private final OrganizationRepository organizationRepository;

    public OrganizationResource(OrganizationService organizationService, OrganizationRepository organizationRepository) {
        this.organizationService = organizationService;
        this.organizationRepository = organizationRepository;
    }

    @Operation(summary = "Create an organization")
    @ApiResponse(responseCode = "201", description = "Organization created")
    @ApiResponse(responseCode = "400", description = "Already has an ID")
    @PostMapping("")
    public ResponseEntity<OrganizationDTO> createOrganization(@Valid @RequestBody OrganizationDTO organizationDTO)
        throws URISyntaxException {
        log.debug("REST request to save Organization : {}", organizationDTO);
        if (organizationDTO.getId() != null) {
            throw new BadRequestAlertException("A new organization cannot already have an ID", ENTITY_NAME, "idexists");
        }
        OrganizationDTO result = organizationService.save(organizationDTO);
        return ResponseEntity
            .created(new URI("/api/organizations/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update an organization")
    @ApiResponse(responseCode = "200", description = "Organization updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationDTO> updateOrganization(
        @Parameter(description = "Organization ID") @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody OrganizationDTO organizationDTO) {
        log.debug("REST request to update Organization : {}, {}", id, organizationDTO);
        if (!Objects.equals(id, organizationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!organizationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        OrganizationDTO result = organizationService.update(organizationDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, organizationDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update an organization")
    @ApiResponse(responseCode = "200", description = "Organization partially updated")
    @ApiResponse(responseCode = "404", description = "Not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<OrganizationDTO> partialUpdateOrganization(
        @Parameter(description = "Organization ID") @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody OrganizationDTO organizationDTO) {
        log.debug("REST request to partial update Organization partially : {}, {}", id, organizationDTO);
        if (!Objects.equals(id, organizationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!organizationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<OrganizationDTO> result = organizationService.partialUpdate(organizationDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, organizationDTO.getId())
        );
    }

    @Operation(summary = "Get all organizations")
    @ApiResponse(responseCode = "200", description = "List of organizations")
    @GetMapping("")
    public List<OrganizationDTO> getAllOrganizations() {
        log.debug("REST request to get all Organizations");
        return organizationService.findAll();
    }

    @Operation(summary = "Get an organization by ID")
    @ApiResponse(responseCode = "200", description = "Organization found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDTO> getOrganization(
        @Parameter(description = "Organization ID") @PathVariable String id) {
        log.debug("REST request to get Organization : {}", id);
        Optional<OrganizationDTO> organizationDTO = organizationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(organizationDTO);
    }

    @Operation(summary = "Delete an organization")
    @ApiResponse(responseCode = "204", description = "Organization deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(
        @Parameter(description = "Organization ID") @PathVariable String id) {
        log.debug("REST request to delete Organization : {}", id);
        organizationService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
