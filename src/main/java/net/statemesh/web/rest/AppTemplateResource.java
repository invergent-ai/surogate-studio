package net.statemesh.web.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.statemesh.repository.AppTemplateRepository;
import net.statemesh.service.AppTemplateService;
import net.statemesh.service.dto.AppTemplateDTO;
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
 * REST controller for managing {@link net.statemesh.domain.AppTemplate}.
 */
@RestController
@RequestMapping("/api/app-template")
@RequiredArgsConstructor
public class AppTemplateResource {
    private final Logger log = LoggerFactory.getLogger(AppTemplateResource.class);

    private static final String ENTITY_NAME = "app-template";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AppTemplateService appTemplateService;
    private final AppTemplateRepository appTemplateRepository;

    /**
     * {@code POST  /app-template} : Create a new app template.
     */
    @PostMapping("")
    public ResponseEntity<AppTemplateDTO> createAppTemplate(@Valid @RequestBody AppTemplateDTO appTemplateDTO) throws URISyntaxException {
        log.debug("REST request to save app template : {}", appTemplateDTO);
        if (appTemplateDTO.getId() != null) {
            throw new BadRequestAlertException("A new app template cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AppTemplateDTO result = appTemplateService.save(appTemplateDTO);
        return ResponseEntity
            .created(new URI("/api/app-template/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /app-template/:id} : Updates an existing app template.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AppTemplateDTO> updateAppTemplate(
        @PathVariable(value = "id", name = "id", required = false) final String id,
        @Valid @RequestBody AppTemplateDTO appTemplateDTO) {
        log.debug("REST request to update AppTemplate : {}, {}", id, appTemplateDTO);
        if (!Objects.equals(id, appTemplateDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!appTemplateRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        AppTemplateDTO result = appTemplateService.update(appTemplateDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, appTemplateDTO.getId()))
            .body(result);
    }

    /**
     * {@code GET  /app-template} : get all app templates with optional filtering.
     *
     * @param search optional search term for name/description
     * @param category optional category filter
     * @param providerId optional provider filter
     * @param sortBy optional sort field (name, createdDate, etc.)
     * @param sortOrder optional sort direction (asc, desc)
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the filtered list of app templates in body.
     */
    @GetMapping("")
    public List<AppTemplateDTO> getAppTemplates(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String providerId,
        @RequestParam(defaultValue = "name") String sortBy,
        @RequestParam(defaultValue = "asc") String sortOrder) {
        log.debug("REST request to get app templates with filters - search: {}, category: {}, providerId: {}, sortBy: {}, sortOrder: {}",
            search, category, providerId, sortBy, sortOrder);
        return appTemplateService.findAllWithFilters(search, category, providerId, sortBy, sortOrder);
    }

    /**
     * {@code GET  /app-template/all} : get all app templates without filtering (backward compatibility).
     */
    @GetMapping("/all")
    public List<AppTemplateDTO> getAllAppTemplates() {
        log.debug("REST request to get all app templates");
        return appTemplateService.findAll();
    }

    /**
     * {@code GET  /app-template/categories} : get all unique categories.
     */
    @GetMapping("/categories")
    public List<String> getCategories() {
        log.debug("REST request to get all categories");
        return appTemplateService.findAllCategories();
    }

    /**
     * {@code GET  /app-template/:id} : get the "id" app template.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppTemplateDTO> getAppTemplate(@PathVariable(name = "id") String id) {
        log.debug("REST request to get app template : {}", id);
        Optional<AppTemplateDTO> appTemplateDTO = appTemplateService.findOne(id);
        return ResponseUtil.wrapOrNotFound(appTemplateDTO);
    }

    /**
     * {@code DELETE  /app-template/:id} : delete the "id" app template.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppTemplate(@PathVariable(value = "id") String id) {
        log.debug("REST request to delete app template : {}", id);
        appTemplateService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
