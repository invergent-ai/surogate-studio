package net.statemesh.web.rest;

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

/**
 * REST controller for managing {@link net.statemesh.domain.SystemConfiguration}.
 */
@RestController
@RequestMapping("/api/system-configurations")
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

    /**
     * {@code POST  /system-configurations} : Create a new systemConfiguration.
     *
     * @param systemConfigurationDTO the systemConfigurationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new systemConfigurationDTO, or with status {@code 400 (Bad Request)} if the systemConfiguration has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
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

    /**
     * {@code PUT  /system-configurations/:id} : Updates an existing systemConfiguration.
     *
     * @param id the id of the systemConfigurationDTO to save.
     * @param systemConfigurationDTO the systemConfigurationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated systemConfigurationDTO,
     * or with status {@code 400 (Bad Request)} if the systemConfigurationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the systemConfigurationDTO couldn't be updated.
     */
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

    /**
     * {@code PATCH  /system-configurations/:id} : Partial updates given fields of an existing systemConfiguration, field will ignore if it is null
     *
     * @param id the id of the systemConfigurationDTO to save.
     * @param systemConfigurationDTO the systemConfigurationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated systemConfigurationDTO,
     * or with status {@code 400 (Bad Request)} if the systemConfigurationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the systemConfigurationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the systemConfigurationDTO couldn't be updated.
     */
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

    /**
     * {@code GET  /system-configurations} : get all the systemConfigurations.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of systemConfigurations in body.
     */
    @GetMapping("")
    public List<SystemConfigurationDTO> getAllSystemConfigurations() {
        log.debug("REST request to get all SystemConfigurations");
        return systemConfigurationService.findAll();
    }

    /**
     * {@code GET  /system-configurations/:id} : get the "id" systemConfiguration.
     *
     * @param id the id of the systemConfigurationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the systemConfigurationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<SystemConfigurationDTO> getSystemConfiguration(@PathVariable String id) {
        log.debug("REST request to get SystemConfiguration : {}", id);
        Optional<SystemConfigurationDTO> systemConfigurationDTO = systemConfigurationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(systemConfigurationDTO);
    }

    /**
     * {@code DELETE  /system-configurations/:id} : delete the "id" systemConfiguration.
     *
     * @param id the id of the systemConfigurationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
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
