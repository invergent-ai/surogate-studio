package net.statemesh.web.rest;

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

/**
 * REST controller for managing {@link net.statemesh.domain.Database}.
 */
@RestController
@RequestMapping("/api/databases")
@RequiredArgsConstructor
@Slf4j
public class DatabaseResource {
    private static final String ENTITY_NAME = "database";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final DatabaseService databaseService;
    private final DatabaseQueryService databaseQueryService;

    /**
     * {@code POST  /databases} : Create a new database.
     *
     * @param databaseDTO the databaseDTO to create or update.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new databaseDTO, or with status {@code 400 (Bad Request)} if the database has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
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

    @PostMapping("/password")
    public ResponseEntity<StringDTO> password(@RequestBody DatabaseDTO databaseDTO) throws URISyntaxException {
        log.debug("REST request to get password for Database : {}", databaseDTO);
        return ResponseEntity.ok(new StringDTO(databaseService.getDatabasePassword(databaseDTO)));
    }

    /**
     * {@code DELETE  /databases/:id} : delete the "id" database.
     *
     * @param id the id of the databaseDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDatabase(@PathVariable(name = "id") String id,
                                               Principal principal) {
        log.debug("REST request to delete Database : {}", id);
        databaseService.delete(id, principal.getName(), Boolean.FALSE);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @DeleteMapping("/keep/{id}")
    public ResponseEntity<Void> deleteDatabaseKeep(@PathVariable(name = "id") String id,
                                                   Principal principal) {
        log.debug("REST request to delete Database and keep data : {}", id);
        databaseService.delete(id, principal.getName(), Boolean.TRUE);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<DatabaseDTO>> searchDatabases(@RequestParam("query") String query) {
        return ResponseEntity.ok(databaseService.searchByName(query));
    }

    /**
     * {@code GET  /databases/:id} : get the "id" database.
     *
     * @param id the id of the databaseDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the databaseDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DatabaseDTO> getDatabase(@PathVariable(name = "id") String id) {
        log.debug("REST request to get Database : {}", id);
        Optional<DatabaseDTO> databaseDTO = databaseService.findOne(id);
        return ResponseUtil.wrapOrNotFound(databaseDTO);
    }

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
