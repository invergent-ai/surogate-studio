package net.statemesh.web.rest;

import io.kubernetes.client.openapi.ApiException;
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

/**
 * REST controller for managing {@link net.statemesh.domain.Node}.
 */
@RestController
@RequestMapping("/api/nodes")
@RequiredArgsConstructor
@Slf4j
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

    /**
     * {@code POST  /nodes} : Create a new node.
     *
     * @param nodeDTO the nodeDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new nodeDTO, or with status {@code 400 (Bad Request)} if the node has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<NodeDTO> createNode(@Valid @RequestBody NodeDTO nodeDTO,
                                              Principal principal) throws URISyntaxException {
        log.debug("REST request to save Node : {}", nodeDTO);
        if (nodeDTO.getId() != null) {
            throw new BadRequestAlertException("A new node cannot already have an ID", ENTITY_NAME, "idexists");
        }

        nodeDTO.setUser(
            userService.findOne(principal.getName()).orElse(null)
        );
        NodeDTO result = nodeService.save(nodeDTO);
        return ResponseEntity
            .created(new URI("/api/nodes/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /nodes/:id} : Updates an existing node.
     *
     * @param id the id of the nodeDTO to save.
     * @param nodeDTO the nodeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated nodeDTO,
     * or with status {@code 400 (Bad Request)} if the nodeDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the nodeDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<NodeDTO> updateNode(
        @PathVariable(value = "id", required = false) final String id,
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

    /**
     * {@code PATCH  /nodes/:id} : Partial updates given fields of an existing node, field will ignore if it is null
     *
     * @param id the id of the nodeDTO to save.
     * @param nodeDTO the nodeDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated nodeDTO,
     * or with status {@code 400 (Bad Request)} if the nodeDTO is not valid,
     * or with status {@code 404 (Not Found)} if the nodeDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the nodeDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<NodeDTO> partialUpdateNode(
        @PathVariable(value = "id", required = false) final String id,
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

    /**
     * {@code GET  /nodes} : get all the nodes.
     *
     * @param pageable the pagination information.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of nodes in body.
     */
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

    /**
     * {@code GET  /nodes/:id} : get the "id" node.
     *
     * @param id the id of the nodeDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the nodeDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<NodeDTO> getNode(@PathVariable String id) {
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

    /**
     * {@code DELETE  /nodes/:id} : delete the "id" node.
     *
     * @param id the id of the nodeDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable(value = "id") String id) {
        log.debug("REST request to delete Node : {}", id);
        nodeControlService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @PostMapping("/del")
    public ResponseEntity<Void> deleteNodes(@RequestBody IdCollection idCollection) {
        log.debug("REST request to delete Nodes : {}", idCollection.getIds());
        nodeService.deleteAll(idCollection.getIds());
        return ResponseEntity
            .noContent()
            .build();
    }
}
