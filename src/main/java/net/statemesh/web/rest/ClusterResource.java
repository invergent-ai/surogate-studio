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

@RestController
@RequestMapping("/api/clusters")
@Tag(name = "Clusters", description = "Kubernetes cluster management")
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

    @Operation(summary = "Create a new cluster")
    @ApiResponse(responseCode = "201", description = "Cluster created")
    @ApiResponse(responseCode = "400", description = "Cluster already has an ID")
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

    @Operation(summary = "Update an existing cluster")
    @ApiResponse(responseCode = "200", description = "Cluster updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<ClusterDTO> updateCluster(
        @Parameter(description = "Cluster ID") @PathVariable(value = "id", name = "id", required = false) final String id,
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

    @Operation(summary = "Partially update a cluster")
    @ApiResponse(responseCode = "200", description = "Cluster partially updated")
    @ApiResponse(responseCode = "404", description = "Cluster not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<ClusterDTO> partialUpdateCluster(
        @Parameter(description = "Cluster ID") @PathVariable(value = "id", name = "id", required = false) final String id,
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

    @Operation(summary = "Get all clusters")
    @ApiResponse(responseCode = "200", description = "List of clusters")
    @GetMapping("")
    public List<ClusterDTO> getAllClusters() {
        log.debug("REST request to get all Clusters");
        return clusterService.findAll();
    }

    @Operation(summary = "Get a cluster by ID")
    @ApiResponse(responseCode = "200", description = "Cluster found")
    @ApiResponse(responseCode = "404", description = "Cluster not found")
    @GetMapping("/{id}")
    public ResponseEntity<ClusterDTO> getCluster(
        @Parameter(description = "Cluster ID") @PathVariable(name = "id") String id) {
        log.debug("REST request to get Cluster : {}", id);
        Optional<ClusterDTO> clusterDTO = clusterService.findOne(id);
        return ResponseUtil.wrapOrNotFound(clusterDTO);
    }

    @Operation(summary = "Delete a cluster")
    @ApiResponse(responseCode = "204", description = "Cluster deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCluster(
        @Parameter(description = "Cluster ID") @PathVariable(value = "id") String id) {
        log.debug("REST request to delete Cluster : {}", id);
        clusterService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
