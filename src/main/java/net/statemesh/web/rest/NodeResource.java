package net.statemesh.web.rest;

import io.kubernetes.client.openapi.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.k8s.KubernetesController;
import net.statemesh.repository.NodeRepository;
import net.statemesh.service.NodeService;
import net.statemesh.service.UserService;
import net.statemesh.service.criteria.NodeCriteria;
import net.statemesh.service.dto.NodeDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import net.statemesh.service.k8s.NodeControlService;
import net.statemesh.service.query.NodeQueryService;
import net.statemesh.web.rest.vm.IdCollection;
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

@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Nodes", description = "Kubernetes node management")
public class NodeResource {
    private static final String ENTITY_NAME = "node";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final NodeService nodeService;
    private final NodeControlService nodeControlService;
    private final NodeQueryService nodeQueryService;
    private final UserService userService;
    private final NodeRepository nodeRepository;
    private final KubernetesController kubernetesController;

    @Operation(summary = "Create a new node")
    @ApiResponse(responseCode = "201", description = "Node created")
    @ApiResponse(responseCode = "400", description = "Node already has an ID")
    @PostMapping("")
    public ResponseEntity<NodeDTO> createNode(@Valid @RequestBody NodeDTO nodeDTO,
                                              Principal principal) throws URISyntaxException {
        log.debug("REST request to save Node : {}", nodeDTO);
        if (nodeDTO.getId() != null) {
            throw new BadRequestAlertException("A new node cannot already have an ID", ENTITY_NAME, "idexists");
        }
        nodeDTO.setUser(userService.findOne(principal.getName()).orElse(null));
        NodeDTO result = nodeService.save(nodeDTO);
        return ResponseEntity
            .created(new URI("/api/nodes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update an existing node")
    @ApiResponse(responseCode = "200", description = "Node updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<NodeDTO> updateNode(
        @Parameter(description = "Node ID") @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody NodeDTO nodeDTO) {
        log.debug("REST request to update Node : {}, {}", id, nodeDTO);
        if (nodeDTO.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, nodeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!nodeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        NodeDTO result = nodeService.update(nodeDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, nodeDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update a node")
    @ApiResponse(responseCode = "200", description = "Node partially updated")
    @ApiResponse(responseCode = "404", description = "Node not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<NodeDTO> partialUpdateNode(
        @Parameter(description = "Node ID") @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody NodeDTO nodeDTO) {
        log.debug("REST request to partial update Node partially : {}, {}", id, nodeDTO);
        if (!Objects.equals(id, nodeDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!nodeRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<NodeDTO> result = nodeService.partialUpdate(nodeDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, nodeDTO.getId())
        );
    }

    @Operation(summary = "Query nodes with criteria (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated nodes returned")
    @GetMapping("")
    public ResponseEntity<List<NodeDTO>> getAllNodes(
        NodeCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        Principal principal
    ) {
        log.trace("REST request to get a page of Nodes");
        Page<NodeDTO> page = nodeQueryService.findByCriteria(criteria, pageable, principal.getName());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @Operation(summary = "Get a node by ID")
    @ApiResponse(responseCode = "200", description = "Node found")
    @ApiResponse(responseCode = "404", description = "Node not found")
    @GetMapping("/{id}")
    public ResponseEntity<NodeDTO> getNode(
        @Parameter(description = "Node ID") @PathVariable String id) {
        log.debug("REST request to get Node : {}", id);
        Optional<NodeDTO> nodeDTO = nodeService.findOne(id);
        return ResponseUtil.wrapOrNotFound(
            nodeDTO.map(dto -> {
                try {
                    var ready = kubernetesController.isNodeReady(dto);
                    dto.setReady(ready);
                } catch (ApiException e) {
                    log.error("Error getting node status", e);
                }
                return dto;
            })
        );
    }

    @Operation(summary = "Delete a node")
    @ApiResponse(responseCode = "204", description = "Node deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNode(
        @Parameter(description = "Node ID") @PathVariable(value = "id") String id) {
        log.debug("REST request to delete Node : {}", id);
        nodeControlService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @Operation(summary = "Delete multiple nodes")
    @ApiResponse(responseCode = "204", description = "Nodes deleted")
    @PostMapping("/del")
    public ResponseEntity<Void> deleteNodes(@RequestBody IdCollection idCollection) {
        log.debug("REST request to delete Nodes : {}", idCollection.getIds());
        nodeService.deleteAll(idCollection.getIds());
        return ResponseEntity.noContent().build();
    }
}
