package net.statemesh.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.repository.ContainerRepository;
import net.statemesh.service.query.ContainerQueryService;
import net.statemesh.service.ContainerService;
import net.statemesh.service.criteria.ContainerCriteria;
import net.statemesh.service.dto.ContainerDTO;
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
 * REST controller for managing {@link net.statemesh.domain.Container}.
 */
@RestController
@RequestMapping("/api/containers")
@Slf4j
@RequiredArgsConstructor
public class ContainerResource {
    private static final String ENTITY_NAME = "container";

    private final ContainerService containerService;
    private final ContainerRepository containerRepository;
    private final ContainerQueryService containerQueryService;

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    /**
     * {@code POST  /containers} : Create a new container.
     *
     * @param containerDTO the containerDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new containerDTO, or with status {@code 400 (Bad Request)} if the container has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ContainerDTO> createContainer(@Valid @RequestBody ContainerDTO containerDTO) throws URISyntaxException {
        log.debug("REST request to save Container : {}", containerDTO);
        if (containerDTO.getId() != null) {
            throw new BadRequestAlertException("A new container cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ContainerDTO result = containerService.save(containerDTO);
        return ResponseEntity
            .created(new URI("/api/containers/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /containers/:id} : Updates an existing container.
     *
     * @param id the id of the containerDTO to save.
     * @param containerDTO the containerDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated containerDTO,
     * or with status {@code 400 (Bad Request)} if the containerDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the containerDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ContainerDTO> updateContainer(
        @PathVariable(value = "id", name = "id", required = false) final String id,
        @Valid @RequestBody ContainerDTO containerDTO) {
        log.debug("REST request to update Container : {}, {}", id, containerDTO);
        if (!Objects.equals(id, containerDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!containerRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ContainerDTO result = containerService.update(containerDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, containerDTO.getId()))
            .body(result);
    }

    /**
     * {@code PATCH  /containers/:id} : Partial updates given fields of an existing container, field will ignore if it is null
     *
     * @param id the id of the containerDTO to save.
     * @param containerDTO the containerDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated containerDTO,
     * or with status {@code 400 (Bad Request)} if the containerDTO is not valid,
     * or with status {@code 404 (Not Found)} if the containerDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the containerDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ContainerDTO> partialUpdateContainer(
        @PathVariable(value = "id", name = "id", required = false) final String id,
        @NotNull @RequestBody ContainerDTO containerDTO) {
        log.debug("REST request to partial update Container partially : {}, {}", id, containerDTO);
        if (!Objects.equals(id, containerDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!containerRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ContainerDTO> result = containerService.partialUpdate(containerDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, containerDTO.getId())
        );
    }

    /**
     * {@code GET  /containers} : get all the containers.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of containers in body.
     */
    @GetMapping("/all")
    public List<ContainerDTO> getAllContainers() {
        log.debug("REST request to get all Containers");
        return containerService.findAll();
    }

    /**
     * {@code GET  /containers/:id} : get the "id" container.
     *
     * @param id the id of the containerDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the containerDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ContainerDTO> getContainer(@PathVariable(name = "id") String id) {
        log.debug("REST request to get Container : {}", id);
        Optional<ContainerDTO> containerDTO = containerService.findOne(id);
        return ResponseUtil.wrapOrNotFound(containerDTO);
    }

    /**
     * {@code DELETE  /containers/:id} : delete the "id" container.
     *
     * @param id the id of the containerDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteContainer(@PathVariable(name = "id") String id) {
        log.debug("REST request to delete Container : {}", id);
        containerService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @GetMapping("/by-application/{id}")
    public ResponseEntity<List<ContainerDTO>> getContainersByPodConfig(@PathVariable(value = "id", name = "id") String applicationId) {
        log.debug("REST request to get Containers by Application ID : {}", applicationId);
        List<ContainerDTO> containers = containerService.findAllByApplicationId(applicationId);
        return ResponseEntity.ok().body(containers);
    }

    @GetMapping("")
    public ResponseEntity<List<ContainerDTO>> getAllContainers(
        ContainerCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get Containers by criteria: {}", criteria);

        if (criteria == null || criteria.getApplicationId() == null || criteria.getApplicationId().getEquals() == null) {
            throw new BadRequestAlertException("Application ID is required", ENTITY_NAME, "applicationidrequired");
        }

        Page<ContainerDTO> resultPage = containerQueryService.findByCriteria(criteria, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), resultPage);
        return ResponseEntity.ok().headers(headers).body(resultPage.getContent());
    }
}
