package net.statemesh.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.statemesh.repository.ProtocolRepository;
import net.statemesh.service.ProtocolService;
import net.statemesh.service.dto.ProtocolDTO;
import net.statemesh.service.exception.BadRequestAlertException;
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

/**
 * REST controller for managing {@link net.statemesh.domain.Protocol}.
 */
@RestController
@RequestMapping("/api/protocols")
public class ProtocolResource {

    private final Logger log = LoggerFactory.getLogger(ProtocolResource.class);

    private static final String ENTITY_NAME = "protocol";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProtocolService protocolService;

    private final ProtocolRepository protocolRepository;

    public ProtocolResource(ProtocolService protocolService, ProtocolRepository protocolRepository) {
        this.protocolService = protocolService;
        this.protocolRepository = protocolRepository;
    }

    /**
     * {@code POST  /protocols} : Create a new protocol.
     *
     * @param protocolDTO the protocolDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new protocolDTO, or with status {@code 400 (Bad Request)} if the protocol has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ProtocolDTO> createProtocol(@Valid @RequestBody ProtocolDTO protocolDTO) throws URISyntaxException {
        log.debug("REST request to save Protocol : {}", protocolDTO);
        if (protocolDTO.getId() != null) {
            throw new BadRequestAlertException("A new protocol cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ProtocolDTO result = protocolService.save(protocolDTO);
        return ResponseEntity
            .created(new URI("/api/protocols/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /protocols/:id} : Updates an existing protocol.
     *
     * @param id the id of the protocolDTO to save.
     * @param protocolDTO the protocolDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated protocolDTO,
     * or with status {@code 400 (Bad Request)} if the protocolDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the protocolDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ProtocolDTO> updateProtocol(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody ProtocolDTO protocolDTO) {
        log.debug("REST request to update Protocol : {}, {}", id, protocolDTO);
        if (!Objects.equals(id, protocolDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!protocolRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ProtocolDTO result = protocolService.update(protocolDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, protocolDTO.getId()))
            .body(result);
    }

    /**
     * {@code PATCH  /protocols/:id} : Partial updates given fields of an existing protocol, field will ignore if it is null
     *
     * @param id the id of the protocolDTO to save.
     * @param protocolDTO the protocolDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated protocolDTO,
     * or with status {@code 400 (Bad Request)} if the protocolDTO is not valid,
     * or with status {@code 404 (Not Found)} if the protocolDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the protocolDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ProtocolDTO> partialUpdateProtocol(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody ProtocolDTO protocolDTO) {
        log.debug("REST request to partial update Protocol partially : {}, {}", id, protocolDTO);
        if (!Objects.equals(id, protocolDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!protocolRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ProtocolDTO> result = protocolService.partialUpdate(protocolDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, protocolDTO.getId())
        );
    }

    /**
     * {@code GET  /protocols} : get all the protocols.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of protocols in body.
     */
    @GetMapping("")
    public ResponseEntity<List<ProtocolDTO>> getAllProtocols(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Protocols");
        Page<ProtocolDTO> page = protocolService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    /**
     * {@code GET  /protocols/:id} : get the "id" protocol.
     *
     * @param id the id of the protocolDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the protocolDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProtocolDTO> getProtocol(@PathVariable String id) {
        log.debug("REST request to get Protocol : {}", id);
        Optional<ProtocolDTO> protocolDTO = protocolService.findOne(id);
        return ResponseUtil.wrapOrNotFound(protocolDTO);
    }

    /**
     * {@code DELETE  /protocols/:id} : delete the "id" protocol.
     *
     * @param id the id of the protocolDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProtocol(@PathVariable String id) {
        log.debug("REST request to delete Protocol : {}", id);
        protocolService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
