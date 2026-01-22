package net.statemesh.web.rest;

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

/**
 * REST controller for managing {@link net.statemesh.domain.Zone}.
 */
@RestController
@RequestMapping("/api/zones")
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

    /**
     * {@code POST  /zones} : Create a new zone.
     *
     * @param zoneDTO the zoneDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new zoneDTO, or with status {@code 400 (Bad Request)} if the zone has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
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

    /**
     * {@code PUT  /zones/:id} : Updates an existing zone.
     *
     * @param id the id of the zoneDTO to save.
     * @param zoneDTO the zoneDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated zoneDTO,
     * or with status {@code 400 (Bad Request)} if the zoneDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the zoneDTO couldn't be updated.
     */
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

    /**
     * {@code PATCH  /zones/:id} : Partial updates given fields of an existing zone, field will ignore if it is null
     *
     * @param id the id of the zoneDTO to save.
     * @param zoneDTO the zoneDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated zoneDTO,
     * or with status {@code 400 (Bad Request)} if the zoneDTO is not valid,
     * or with status {@code 404 (Not Found)} if the zoneDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the zoneDTO couldn't be updated.
     */
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

    /**
     * {@code GET  /zones} : get all the zones.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of zones in body.
     */
    @GetMapping("")
    public List<ZoneDTO> getAllZones() {
        log.trace("REST request to get all Zones");
        return zoneService.findAll();
    }

    /**
     * {@code GET  /zones/:id} : get the "id" zone.
     *
     * @param id the id of the zoneDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the zoneDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ZoneDTO> getZone(@PathVariable String id) {
        log.debug("REST request to get Zone : {}", id);
        Optional<ZoneDTO> zoneDTO = zoneService.findOne(id);
        return ResponseUtil.wrapOrNotFound(zoneDTO);
    }

    /**
     * {@code DELETE  /zones/:id} : delete the "id" zone.
     *
     * @param id the id of the zoneDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
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
