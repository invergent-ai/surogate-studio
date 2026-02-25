package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Applications", description = "Application deployment and lifecycle management")
public class ApplicationResource {
    private static final String ENTITY_NAME = "application";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ApplicationService applicationService;
    private final ApplicationQueryService applicationQueryService;

    @Operation(summary = "Save an application")
    @ApiResponse(responseCode = "201", description = "Application saved")
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

    @Operation(summary = "Deploy an application")
    @ApiResponse(responseCode = "201", description = "Application deployed")
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

    @Operation(summary = "Redeploy an application")
    @ApiResponse(responseCode = "201", description = "Application redeployed")
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

    @Operation(summary = "Delete an application")
    @ApiResponse(responseCode = "204", description = "Application deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteApplication(
        @Parameter(description = "Application ID") @PathVariable(name = "id") String id,
        Principal principal) {
        log.debug("REST request to delete Application : {}", id);
        applicationService.delete(id, principal.getName(), Boolean.FALSE);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @Operation(summary = "Delete an application keeping data")
    @ApiResponse(responseCode = "204", description = "Application deleted, data retained")
    @DeleteMapping("/keep/{id}")
    public ResponseEntity<Void> deleteApplicationKeep(
        @Parameter(description = "Application ID") @PathVariable(name = "id") String id,
        Principal principal) {
        log.debug("REST request to delete Application and keep data : {}", id);
        applicationService.delete(id, principal.getName(), Boolean.TRUE);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @Operation(summary = "Search applications by name")
    @ApiResponse(responseCode = "200", description = "Applications returned")
    @GetMapping("/search")
    public ResponseEntity<List<ApplicationDTO>> searchApplications(
        @Parameter(description = "Search query") @RequestParam("query") String query) {
        return ResponseEntity.ok(applicationService.searchByName(query));
    }

    @Operation(summary = "Get all applications basic info")
    @ApiResponse(responseCode = "200", description = "Basic info returned")
    @GetMapping("/basic")
    public ResponseEntity<List<ApplicationDTO>> getBasicInfo() {
        return ResponseEntity.ok(applicationService.findAllBasicInfo());
    }

    @Operation(summary = "Get an application by ID")
    @ApiResponse(responseCode = "200", description = "Application found")
    @ApiResponse(responseCode = "404", description = "Application not found")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationDTO> getApplication(
        @Parameter(description = "Application ID") @PathVariable(name = "id") String id) {
        log.debug("REST request to get Application : {}", id);
        Optional<ApplicationDTO> applicationDTO = applicationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(applicationDTO);
    }

    @Operation(summary = "Create application (admin)")
    @ApiResponse(responseCode = "201", description = "Application created")
    @ApiResponse(responseCode = "400", description = "Application already has an ID")
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

    @Operation(summary = "Query applications with criteria")
    @ApiResponse(responseCode = "200", description = "Paginated applications returned")
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
