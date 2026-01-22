package net.statemesh.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.statemesh.repository.ClusterRepository;
import net.statemesh.service.ClusterService;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link net.statemesh.domain.Cluster}.
 */
@RestController
@RequestMapping("/api/clusters")
public class ClusterResource {
    private final Logger log = LoggerFactory.getLogger(ClusterResource.class);

    private static final String ENTITY_NAME = "cluster";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ClusterService clusterService;

    private final ClusterRepository clusterRepository;

    public ClusterResource(ClusterService clusterService, ClusterRepository clusterRepository) {
        this.clusterService = clusterService;
        this.clusterRepository = clusterRepository;
    }

    /**
     * {@code POST  /clusters} : Create a new cluster.
     *
     * @param clusterDTO the clusterDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new clusterDTO, or with status {@code 400 (Bad Request)} if the cluster has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ClusterDTO> createCluster(@Valid @RequestBody ClusterDTO clusterDTO) throws URISyntaxException {
        log.debug("REST request to save Cluster : {}", clusterDTO);
        if (clusterDTO.getId() != null) {
            throw new BadRequestAlertException("A new cluster cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ClusterDTO result = clusterService.save(clusterDTO);
        return ResponseEntity
            .created(new URI("/api/clusters/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /clusters/:id} : Updates an existing cluster.
     *
     * @param id the id of the clusterDTO to save.
     * @param clusterDTO the clusterDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated clusterDTO,
     * or with status {@code 400 (Bad Request)} if the clusterDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the clusterDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClusterDTO> updateCluster(
        @PathVariable(value = "id", name = "id", required = false) final String id,
        @Valid @RequestBody ClusterDTO clusterDTO) {
        log.debug("REST request to update Cluster : {}, {}", id, clusterDTO);
        if (!Objects.equals(id, clusterDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!clusterRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        ClusterDTO result = clusterService.update(clusterDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, clusterDTO.getId()))
            .body(result);
    }

    /**
     * {@code PATCH  /clusters/:id} : Partial updates given fields of an existing cluster, field will ignore if it is null
     *
     * @param id the id of the clusterDTO to save.
     * @param clusterDTO the clusterDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated clusterDTO,
     * or with status {@code 400 (Bad Request)} if the clusterDTO is not valid,
     * or with status {@code 404 (Not Found)} if the clusterDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the clusterDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ClusterDTO> partialUpdateCluster(
        @PathVariable(value = "id", name = "id", required = false) final String id,
        @NotNull @RequestBody ClusterDTO clusterDTO) {
        log.debug("REST request to partial update Cluster partially : {}, {}", id, clusterDTO);
        if (!Objects.equals(id, clusterDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!clusterRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<ClusterDTO> result = clusterService.partialUpdate(clusterDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, clusterDTO.getId())
        );
    }

    /**
     * {@code GET  /clusters} : get all the clusters.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of clusters in body.
     */
    @GetMapping("")
    public List<ClusterDTO> getAllClusters() {
        log.debug("REST request to get all Clusters");
        return clusterService.findAll();
    }

    /**
     * {@code GET  /clusters/:id} : get the "id" cluster.
     *
     * @param id the id of the clusterDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the clusterDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClusterDTO> getCluster(@PathVariable(name = "id") String id) {
        log.debug("REST request to get Cluster : {}", id);
        Optional<ClusterDTO> clusterDTO = clusterService.findOne(id);
        return ResponseUtil.wrapOrNotFound(clusterDTO);
    }

    /**
     * {@code DELETE  /clusters/:id} : delete the "id" cluster.
     *
     * @param id the id of the clusterDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCluster(@PathVariable(value = "id") String id) {
        log.debug("REST request to delete Cluster : {}", id);
        clusterService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
