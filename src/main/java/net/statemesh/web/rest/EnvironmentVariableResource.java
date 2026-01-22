package net.statemesh.web.rest;

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

/**
 * REST controller for managing {@link net.statemesh.domain.EnvironmentVariable}.
 */
@RestController
@RequestMapping("/api/environment-variables")
@RequiredArgsConstructor
public class EnvironmentVariableResource {
    private final Logger log = LoggerFactory.getLogger(EnvironmentVariableResource.class);

    private static final String ENTITY_NAME = "environmentVariable";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final EnvironmentVariableService environmentVariableService;
    private final EnvironmentVariableRepository environmentVariableRepository;

    /**
     * {@code POST  /environment-variables} : Create a new environmentVariable.
     *
     * @param environmentVariableDTO the environmentVariableDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new environmentVariableDTO, or with status {@code 400 (Bad Request)} if the environmentVariable has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
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

    /**
     * {@code PUT  /environment-variables/:id} : Updates an existing environmentVariable.
     *
     * @param id the id of the environmentVariableDTO to save.
     * @param environmentVariableDTO the environmentVariableDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated environmentVariableDTO,
     * or with status {@code 400 (Bad Request)} if the environmentVariableDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the environmentVariableDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<EnvironmentVariableDTO> updateEnvironmentVariable(
        @PathVariable(value = "id", required = false) final String id,
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

    /**
     * {@code PATCH  /environment-variables/:id} : Partial updates given fields of an existing environmentVariable, field will ignore if it is null
     *
     * @param id the id of the environmentVariableDTO to save.
     * @param environmentVariableDTO the environmentVariableDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated environmentVariableDTO,
     * or with status {@code 400 (Bad Request)} if the environmentVariableDTO is not valid,
     * or with status {@code 404 (Not Found)} if the environmentVariableDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the environmentVariableDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<EnvironmentVariableDTO> partialUpdateEnvironmentVariable(
        @PathVariable(value = "id", required = false) final String id,
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

    /**
     * {@code GET  /environment-variables} : get all the environmentVariables.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of environmentVariables in body.
     */
    @GetMapping("")
    public List<EnvironmentVariableDTO> getAllEnvironmentVariables() {
        log.debug("REST request to get all EnvironmentVariables");
        return environmentVariableService.findAll();
    }

    /**
     * {@code GET  /environment-variables/:id} : get the "id" environmentVariable.
     *
     * @param id the id of the environmentVariableDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the environmentVariableDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<EnvironmentVariableDTO> getEnvironmentVariable(@PathVariable String id) {
        log.debug("REST request to get EnvironmentVariable : {}", id);
        Optional<EnvironmentVariableDTO> environmentVariableDTO = environmentVariableService.findOne(id);
        return ResponseUtil.wrapOrNotFound(environmentVariableDTO);
    }

    /**
     * {@code DELETE  /environment-variables/:id} : delete the "id" environmentVariable.
     *
     * @param id the id of the environmentVariableDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEnvironmentVariable(@PathVariable String id) {
        log.debug("REST request to delete EnvironmentVariable : {}", id);
        environmentVariableService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
