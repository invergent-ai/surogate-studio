package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/protocols")
@Tag(name = "Protocol", description = "Protocol management")
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

    @Operation(summary = "Create a new protocol")
    @ApiResponse(responseCode = "201", description = "Protocol created")
    @ApiResponse(responseCode = "400", description = "Invalid input or ID already exists")
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

    @Operation(summary = "Update an existing protocol")
    @ApiResponse(responseCode = "200", description = "Protocol updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
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

    @Operation(summary = "Partially update a protocol")
    @ApiResponse(responseCode = "200", description = "Protocol partially updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
    @ApiResponse(responseCode = "404", description = "Protocol not found")
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

    @Operation(summary = "Get all protocols")
    @ApiResponse(responseCode = "200", description = "List of protocols returned")
    @GetMapping("")
    public ResponseEntity<List<ProtocolDTO>> getAllProtocols(@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Protocols");
        Page<ProtocolDTO> page = protocolService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @Operation(summary = "Get a protocol by ID")
    @ApiResponse(responseCode = "200", description = "Protocol returned")
    @ApiResponse(responseCode = "404", description = "Protocol not found")
    @GetMapping("/{id}")
    public ResponseEntity<ProtocolDTO> getProtocol(@PathVariable String id) {
        log.debug("REST request to get Protocol : {}", id);
        Optional<ProtocolDTO> protocolDTO = protocolService.findOne(id);
        return ResponseUtil.wrapOrNotFound(protocolDTO);
    }

    @Operation(summary = "Delete a protocol")
    @ApiResponse(responseCode = "204", description = "Protocol deleted")
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
