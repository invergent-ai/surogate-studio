package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/app-template")
@RequiredArgsConstructor
@Tag(name = "App Templates", description = "Application template management")
public class AppTemplateResource {
    private final Logger log = LoggerFactory.getLogger(AppTemplateResource.class);
    private static final String ENTITY_NAME = "app-template";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AppTemplateService appTemplateService;
    private final AppTemplateRepository appTemplateRepository;

    @Operation(summary = "Create a new app template")
    @ApiResponse(responseCode = "201", description = "Template created")
    @ApiResponse(responseCode = "400", description = "Template already has an ID")
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

    @Operation(summary = "Update an existing app template")
    @ApiResponse(responseCode = "200", description = "Template updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<AppTemplateDTO> updateAppTemplate(
        @Parameter(description = "Template ID") @PathVariable(value = "id", name = "id", required = false) final String id,
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

    @Operation(summary = "Get app templates with filters")
    @ApiResponse(responseCode = "200", description = "Filtered templates returned")
    @GetMapping("")
    public List<AppTemplateDTO> getAppTemplates(
        @Parameter(description = "Search term") @RequestParam(required = false) String search,
        @Parameter(description = "Category filter") @RequestParam(required = false) String category,
        @Parameter(description = "Provider ID filter") @RequestParam(required = false) String providerId,
        @Parameter(description = "Sort field") @RequestParam(defaultValue = "name") String sortBy,
        @Parameter(description = "Sort direction") @RequestParam(defaultValue = "asc") String sortOrder) {
        log.debug("REST request to get app templates with filters - search: {}, category: {}, providerId: {}, sortBy: {}, sortOrder: {}",
            search, category, providerId, sortBy, sortOrder);
        return appTemplateService.findAllWithFilters(search, category, providerId, sortBy, sortOrder);
    }

    @Operation(summary = "Get all app templates (unfiltered)")
    @ApiResponse(responseCode = "200", description = "All templates returned")
    @GetMapping("/all")
    public List<AppTemplateDTO> getAllAppTemplates() {
        log.debug("REST request to get all app templates");
        return appTemplateService.findAll();
    }

    @Operation(summary = "Get all template categories")
    @ApiResponse(responseCode = "200", description = "Categories returned")
    @GetMapping("/categories")
    public List<String> getCategories() {
        log.debug("REST request to get all categories");
        return appTemplateService.findAllCategories();
    }

    @Operation(summary = "Get an app template by ID")
    @ApiResponse(responseCode = "200", description = "Template found")
    @ApiResponse(responseCode = "404", description = "Template not found")
    @GetMapping("/{id}")
    public ResponseEntity<AppTemplateDTO> getAppTemplate(
        @Parameter(description = "Template ID") @PathVariable(name = "id") String id) {
        log.debug("REST request to get app template : {}", id);
        Optional<AppTemplateDTO> appTemplateDTO = appTemplateService.findOne(id);
        return ResponseUtil.wrapOrNotFound(appTemplateDTO);
    }

    @Operation(summary = "Delete an app template")
    @ApiResponse(responseCode = "204", description = "Template deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppTemplate(
        @Parameter(description = "Template ID") @PathVariable(value = "id") String id) {
        log.debug("REST request to delete app template : {}", id);
        appTemplateService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
