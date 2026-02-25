package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.service.DatabaseService;
import net.statemesh.service.criteria.DatabaseCriteria;
import net.statemesh.service.dto.DatabaseDTO;
import net.statemesh.service.dto.StringDTO;
import net.statemesh.service.query.DatabaseQueryService;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/databases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Databases", description = "Database deployment and lifecycle management")
public class DatabaseResource {
    private static final String ENTITY_NAME = "database";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DatabaseService databaseService;
    private final DatabaseQueryService databaseQueryService;

    @Operation(summary = "Save a database")
    @ApiResponse(responseCode = "201", description = "Database saved")
    @PostMapping("")
    public ResponseEntity<DatabaseDTO> saveDatabase(@RequestBody DatabaseDTO databaseDTO,
                                                    Principal principal) throws URISyntaxException {
        log.debug("REST request to save Database : {}", databaseDTO);
        DatabaseDTO result = databaseService.save(databaseDTO, principal.getName());
        return ResponseEntity
            .created(new URI("/api/databases/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Deploy a database")
    @ApiResponse(responseCode = "201", description = "Database deployed")
    @PostMapping("/deploy")
    public ResponseEntity<DatabaseDTO> deployDatabase(@RequestBody DatabaseDTO databaseDTO,
                                                      Principal principal) throws URISyntaxException {
        log.debug("REST request to deploy Database : {}", databaseDTO);
        DatabaseDTO result = databaseService.deploy(databaseDTO, principal.getName());
        return ResponseEntity
            .created(new URI("/api/databases/deploy" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Redeploy a database")
    @ApiResponse(responseCode = "201", description = "Database redeployed")
    @PostMapping("/redeploy")
    public ResponseEntity<DatabaseDTO> redeployDatabase(@RequestBody DatabaseDTO databaseDTO,
                                                        Principal principal) throws URISyntaxException {
        log.debug("REST request to redeploy Database : {}", databaseDTO);
        DatabaseDTO result = databaseService.redeploy(databaseDTO, principal.getName());
        return ResponseEntity
            .created(new URI("/api/databases/redeploy" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Get database password")
    @ApiResponse(responseCode = "200", description = "Password returned")
    @PostMapping("/password")
    public ResponseEntity<StringDTO> password(@RequestBody DatabaseDTO databaseDTO) throws URISyntaxException {
        log.debug("REST request to get password for Database : {}", databaseDTO);
        return ResponseEntity.ok(new StringDTO(databaseService.getDatabasePassword(databaseDTO)));
    }

    @Operation(summary = "Delete a database")
    @ApiResponse(responseCode = "204", description = "Database deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDatabase(
        @Parameter(description = "Database ID") @PathVariable(name = "id") String id,
        Principal principal) {
        log.debug("REST request to delete Database : {}", id);
        databaseService.delete(id, principal.getName(), Boolean.FALSE);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @Operation(summary = "Delete a database keeping data")
    @ApiResponse(responseCode = "204", description = "Database deleted, data retained")
    @DeleteMapping("/keep/{id}")
    public ResponseEntity<Void> deleteDatabaseKeep(
        @Parameter(description = "Database ID") @PathVariable(name = "id") String id,
        Principal principal) {
        log.debug("REST request to delete Database and keep data : {}", id);
        databaseService.delete(id, principal.getName(), Boolean.TRUE);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @Operation(summary = "Search databases by name")
    @ApiResponse(responseCode = "200", description = "Databases returned")
    @GetMapping("/search")
    public ResponseEntity<List<DatabaseDTO>> searchDatabases(
        @Parameter(description = "Search query") @RequestParam("query") String query) {
        return ResponseEntity.ok(databaseService.searchByName(query));
    }

    @Operation(summary = "Get a database by ID")
    @ApiResponse(responseCode = "200", description = "Database found")
    @ApiResponse(responseCode = "404", description = "Database not found")
    @GetMapping("/{id}")
    public ResponseEntity<DatabaseDTO> getDatabase(
        @Parameter(description = "Database ID") @PathVariable(name = "id") String id) {
        log.debug("REST request to get Database : {}", id);
        Optional<DatabaseDTO> databaseDTO = databaseService.findOne(id);
        return ResponseUtil.wrapOrNotFound(databaseDTO);
    }

    @Operation(summary = "Query databases with criteria (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated databases returned")
    @GetMapping("")
    public ResponseEntity<List<DatabaseDTO>> queryDatabases(
        DatabaseCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        Principal principal
    ) {
        log.debug("REST request to get Databases by criteria: {}", criteria);
        Page<DatabaseDTO> resultPage = databaseQueryService.findByCriteria(criteria, pageable, principal.getName());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), resultPage);
        return ResponseEntity.ok().headers(headers).body(resultPage.getContent());
    }
}
