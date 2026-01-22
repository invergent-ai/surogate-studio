package net.statemesh.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import net.statemesh.repository.VolumeRepository;
import net.statemesh.service.VolumeService;
import net.statemesh.service.criteria.VolumeCriteria;
import net.statemesh.service.dto.VolumeDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import net.statemesh.service.query.VolumeQueryService;
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

/**
 * REST controller for managing {@link net.statemesh.domain.Volume}.
 */
@RestController
@RequestMapping("/api/volumes")
public class VolumeResource {
    private final Logger log = LoggerFactory.getLogger(VolumeResource.class);

    private static final String ENTITY_NAME = "volume";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final VolumeService volumeService;
    private final VolumeRepository volumeRepository;
    private final VolumeQueryService volumeQueryService;

    public VolumeResource(
        VolumeService volumeService,
        VolumeRepository volumeRepository,
        VolumeQueryService volumeQueryService
    ) {
        this.volumeService = volumeService;
        this.volumeRepository = volumeRepository;
        this.volumeQueryService = volumeQueryService;
    }

    /**
     * {@code POST  /volumes} : Create a new volume.
     *
     * @param volumeDTO the volumeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new volumeDTO, or with status {@code 400 (Bad Request)} if the volume has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<VolumeDTO> createVolume(@Valid @RequestBody VolumeDTO volumeDTO, Principal principal) throws URISyntaxException {
        log.debug("REST request to save Volume : {}", volumeDTO);
        if (volumeDTO.getId() != null) {
            throw new BadRequestAlertException("A new volume cannot already have an ID", ENTITY_NAME, "idexists");
        }
        VolumeDTO result = volumeService.save(volumeDTO);

        return ResponseEntity
            .created(new URI("/api/volumes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /volumes/:id} : Updates an existing volume.
     *
     * @param id the id of the volumeDTO to save.
     * @param volumeDTO the volumeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated volumeDTO,
     * or with status {@code 400 (Bad Request)} if the volumeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the volumeDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<VolumeDTO> updateVolume(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody VolumeDTO volumeDTO) {
        log.debug("REST request to update Volume : {}, {}", id, volumeDTO);
        if (!Objects.equals(id, volumeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!volumeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        VolumeDTO result = volumeService.update(volumeDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, volumeDTO.getId()))
            .body(result);
    }

    /**
     * {@code PATCH  /volumes/:id} : Partial updates given fields of an existing volume, field will ignore if it is null
     *
     * @param id the id of the volumeDTO to save.
     * @param volumeDTO the volumeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated volumeDTO,
     * or with status {@code 400 (Bad Request)} if the volumeDTO is not valid,
     * or with status {@code 404 (Not Found)} if the volumeDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the volumeDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<VolumeDTO> partialUpdateVolume(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody VolumeDTO volumeDTO) {
        log.debug("REST request to partial update Volume partially : {}, {}", id, volumeDTO);
        if (!Objects.equals(id, volumeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!volumeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<VolumeDTO> result = volumeService.partialUpdate(volumeDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, volumeDTO.getId())
        );
    }

    @GetMapping("")
    public ResponseEntity<List<VolumeDTO>> queryVolumes(
        VolumeCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        Principal principal
    ) {
        Page<VolumeDTO> resultPage = volumeQueryService.findByCriteria(criteria, pageable, principal.getName());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), resultPage);
        return ResponseEntity.ok().headers(headers).body(resultPage.getContent());
    }

    /**
     * {@code GET  /volumes/:id} : get the "id" volume.
     *
     * @param id the id of the volumeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the volumeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<VolumeDTO> getVolume(@PathVariable String id) {
        log.debug("REST request to get Volume : {}", id);
        Optional<VolumeDTO> volumeDTO = volumeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(volumeDTO);
    }

    /**
     * {@code DELETE  /volumes/:id} : delete the "id" volume.
     *
     * @param id the id of the volumeDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVolume(@PathVariable(name = "id") String id, Principal principal) {
        log.debug("REST request to delete Volume : {}", id);
        volumeService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
