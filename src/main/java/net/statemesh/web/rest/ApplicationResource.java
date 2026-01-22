package net.statemesh.web.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.service.ApplicationService;
import net.statemesh.service.criteria.ApplicationCriteria;
import net.statemesh.service.dto.ApplicationDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import net.statemesh.service.query.ApplicationQueryService;
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
 * REST controller for managing {@link net.statemesh.domain.Application}.
 */
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
public class ApplicationResource {
    private static final String ENTITY_NAME = "application";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ApplicationService applicationService;
    private final ApplicationQueryService applicationQueryService;

    /**
     * {@code POST  /applications} : Create a new application.
     *
     * @param applicationDTO the applicationDTO to create or update.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new applicationDTO, or with status {@code 400 (Bad Request)} if the application has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<ApplicationDTO> saveApplication(@RequestBody ApplicationDTO applicationDTO,
                                                          Principal principal) throws URISyntaxException {
        log.debug("REST request to save Application : {}", applicationDTO);
        ApplicationDTO result = applicationService.save(applicationDTO, principal.getName());
        return ResponseEntity
            .created(new URI("/api/applications/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @PostMapping("/deploy")
    public ResponseEntity<ApplicationDTO> deployApplication(@RequestBody ApplicationDTO applicationDTO,
                                                            Principal principal) throws URISyntaxException {
        log.debug("REST request to deploy Application : {}", applicationDTO);
        ApplicationDTO result = applicationService.deploy(applicationDTO, principal.getName());
        return ResponseEntity
            .created(new URI("/api/applications/deploy" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @PostMapping("/redeploy")
    public ResponseEntity<ApplicationDTO> redeployApplication(@RequestBody ApplicationDTO applicationDTO,
                                                              Principal principal) throws URISyntaxException {
        log.debug("REST request to redeploy Application : {}", applicationDTO);
        ApplicationDTO result = applicationService.redeploy(applicationDTO, principal.getName());
        return ResponseEntity
            .created(new URI("/api/applications/redeploy" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code DELETE  /applications/:id} : delete the "id" application.
     *
     * @param id the id of the applicationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(@PathVariable(name = "id") String id,
                                                  Principal principal) {
        log.debug("REST request to delete Application : {}", id);
        applicationService.delete(id, principal.getName(), Boolean.FALSE);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @DeleteMapping("/keep/{id}")
    public ResponseEntity<Void> deleteApplicationKeep(@PathVariable(name = "id") String id,
                                                      Principal principal) {
        log.debug("REST request to delete Application and keep data : {}", id);
        applicationService.delete(id, principal.getName(), Boolean.TRUE);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ApplicationDTO>> searchApplications(@RequestParam("query") String query) {
        return ResponseEntity.ok(applicationService.searchByName(query));
    }

    @GetMapping("/basic")
    public ResponseEntity<List<ApplicationDTO>> getBasicInfo() {
        return ResponseEntity.ok(applicationService.findAllBasicInfo());
    }

    /**
     * {@code GET  /applications/:id} : get the "id" application.
     *
     * @param id the id of the applicationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the applicationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDTO> getApplication(@PathVariable(name = "id") String id) {
        log.debug("REST request to get Application : {}", id);
        Optional<ApplicationDTO> applicationDTO = applicationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(applicationDTO);
    }

    @PostMapping("/admin")
    public ResponseEntity<ApplicationDTO> createApplicationAdmin(@Valid @RequestBody ApplicationDTO applicationDTO) throws URISyntaxException {
        log.debug("REST request to save Application for admin: {}", applicationDTO);
        if (applicationDTO.getId() != null) {
            throw new BadRequestAlertException("A new application cannot already have an ID", ENTITY_NAME, "idexists");
        }
        ApplicationDTO result = applicationService.saveAdmin(applicationDTO);
        return ResponseEntity
            .created(new URI("/api/applications/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @GetMapping("")
    public ResponseEntity<List<ApplicationDTO>> queryApplications(
        ApplicationCriteria criteria,
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        Principal principal
    ) {
        log.trace("REST request to get Applications by criteria: {}", criteria);
        Page<ApplicationDTO> resultPage = applicationQueryService.findByCriteria(criteria, pageable, principal.getName());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), resultPage);
        return ResponseEntity.ok().headers(headers).body(resultPage.getContent());
    }
}
