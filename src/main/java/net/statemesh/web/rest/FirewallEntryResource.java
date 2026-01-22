package net.statemesh.web.rest;

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

/**
 * REST controller for managing {@link net.statemesh.domain.FirewallEntry}.
 */
@RestController
@RequestMapping("/api/firewall-entries")
@RequiredArgsConstructor
public class FirewallEntryResource {
    private final Logger log = LoggerFactory.getLogger(FirewallEntryResource.class);

    private static final String ENTITY_NAME = "firewallEntry";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final FirewallEntryService firewallEntryService;
    private final FirewallEntryRepository firewallEntryRepository;

    /**
     * {@code POST  /firewall-entries} : Create a new firewallEntry.
     *
     * @param firewallEntryDTO the firewallEntryDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new firewallEntryDTO, or with status {@code 400 (Bad Request)} if the firewallEntry has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
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

    /**
     * {@code PUT  /firewall-entries/:id} : Updates an existing firewallEntry.
     *
     * @param id the id of the firewallEntryDTO to save.
     * @param firewallEntryDTO the firewallEntryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated firewallEntryDTO,
     * or with status {@code 400 (Bad Request)} if the firewallEntryDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the firewallEntryDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FirewallEntryDTO> updateFirewallEntry(
        @PathVariable(value = "id", required = false) final String id,
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

    /**
     * {@code PATCH  /firewall-entries/:id} : Partial updates given fields of an existing firewallEntry, field will ignore if it is null
     *
     * @param id the id of the firewallEntryDTO to save.
     * @param firewallEntryDTO the firewallEntryDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated firewallEntryDTO,
     * or with status {@code 400 (Bad Request)} if the firewallEntryDTO is not valid,
     * or with status {@code 404 (Not Found)} if the firewallEntryDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the firewallEntryDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<FirewallEntryDTO> partialUpdateFirewallEntry(
        @PathVariable(value = "id", required = false) final String id,
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

    /**
     * {@code GET  /firewall-entries} : get all the firewallEntries.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of firewallEntries in body.
     */
    @GetMapping("")
    public List<FirewallEntryDTO> getAllFirewallEntries() {
        log.debug("REST request to get all FirewallEntries");
        return firewallEntryService.findAll();
    }

    /**
     * {@code GET  /firewall-entries/:id} : get the "id" firewallEntry.
     *
     * @param id the id of the firewallEntryDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the firewallEntryDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FirewallEntryDTO> getFirewallEntry(@PathVariable String id) {
        log.debug("REST request to get FirewallEntry : {}", id);
        Optional<FirewallEntryDTO> firewallEntryDTO = firewallEntryService.findOne(id);
        return ResponseUtil.wrapOrNotFound(firewallEntryDTO);
    }

    /**
     * {@code DELETE  /firewall-entries/:id} : delete the "id" firewallEntry.
     *
     * @param id the id of the firewallEntryDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFirewallEntry(@PathVariable String id) {
        log.debug("REST request to delete FirewallEntry : {}", id);
        firewallEntryService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
