package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import net.statemesh.repository.FirewallEntryRepository;
import net.statemesh.service.FirewallEntryService;
import net.statemesh.service.dto.FirewallEntryDTO;
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
@RequestMapping("/api/firewall-entries")
@RequiredArgsConstructor
@Tag(name = "Firewall Entries", description = "Firewall rule management")
public class FirewallEntryResource {
    private final Logger log = LoggerFactory.getLogger(FirewallEntryResource.class);
    private static final String ENTITY_NAME = "firewallEntry";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final FirewallEntryService firewallEntryService;
    private final FirewallEntryRepository firewallEntryRepository;

    @Operation(summary = "Create a firewall entry")
    @ApiResponse(responseCode = "201", description = "Firewall entry created")
    @ApiResponse(responseCode = "400", description = "Already has an ID")
    @PostMapping("")
    public ResponseEntity<FirewallEntryDTO> createFirewallEntry(
        @Valid @RequestBody FirewallEntryDTO firewallEntryDTO
    ) throws URISyntaxException {
        log.debug("REST request to save FirewallEntry : {}", firewallEntryDTO);
        if (firewallEntryDTO.getId() != null) {
            throw new BadRequestAlertException("A new firewallEntry cannot already have an ID", ENTITY_NAME, "idexists");
        }
        FirewallEntryDTO result = firewallEntryService.save(firewallEntryDTO);
        return ResponseEntity
            .created(new URI("/api/firewall-entries/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update a firewall entry")
    @ApiResponse(responseCode = "200", description = "Firewall entry updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<FirewallEntryDTO> updateFirewallEntry(
        @Parameter(description = "Firewall entry ID") @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody FirewallEntryDTO firewallEntryDTO) {
        log.debug("REST request to update FirewallEntry : {}, {}", id, firewallEntryDTO);
        if (!Objects.equals(id, firewallEntryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!firewallEntryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        FirewallEntryDTO result = firewallEntryService.update(firewallEntryDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, firewallEntryDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update a firewall entry")
    @ApiResponse(responseCode = "200", description = "Firewall entry partially updated")
    @ApiResponse(responseCode = "404", description = "Not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<FirewallEntryDTO> partialUpdateFirewallEntry(
        @Parameter(description = "Firewall entry ID") @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody FirewallEntryDTO firewallEntryDTO) {
        log.debug("REST request to partial update FirewallEntry partially : {}, {}", id, firewallEntryDTO);
        if (!Objects.equals(id, firewallEntryDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!firewallEntryRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<FirewallEntryDTO> result = firewallEntryService.partialUpdate(firewallEntryDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, firewallEntryDTO.getId())
        );
    }

    @Operation(summary = "Get all firewall entries")
    @ApiResponse(responseCode = "200", description = "List of firewall entries")
    @GetMapping("")
    public List<FirewallEntryDTO> getAllFirewallEntries() {
        log.debug("REST request to get all FirewallEntries");
        return firewallEntryService.findAll();
    }

    @Operation(summary = "Get a firewall entry by ID")
    @ApiResponse(responseCode = "200", description = "Firewall entry found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public ResponseEntity<FirewallEntryDTO> getFirewallEntry(
        @Parameter(description = "Firewall entry ID") @PathVariable String id) {
        log.debug("REST request to get FirewallEntry : {}", id);
        Optional<FirewallEntryDTO> firewallEntryDTO = firewallEntryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(firewallEntryDTO);
    }

    @Operation(summary = "Delete a firewall entry")
    @ApiResponse(responseCode = "204", description = "Firewall entry deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFirewallEntry(
        @Parameter(description = "Firewall entry ID") @PathVariable String id) {
        log.debug("REST request to delete FirewallEntry : {}", id);
        firewallEntryService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
