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
import net.statemesh.repository.AnnotationRepository;
import net.statemesh.service.AnnotationService;
import net.statemesh.service.dto.AnnotationDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

@RestController
@RequestMapping("/api/annotations")
@Tag(name = "Annotations", description = "CRUD operations for annotations")
public class AnnotationResource {

    private final Logger log = LoggerFactory.getLogger(AnnotationResource.class);
    private static final String ENTITY_NAME = "annotation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final AnnotationService annotationService;
    private final AnnotationRepository annotationRepository;

    public AnnotationResource(AnnotationService annotationService, AnnotationRepository annotationRepository) {
        this.annotationService = annotationService;
        this.annotationRepository = annotationRepository;
    }

    @Operation(summary = "Create a new annotation")
    @ApiResponse(responseCode = "201", description = "Annotation created")
    @ApiResponse(responseCode = "400", description = "Annotation already has an ID")
    @PostMapping("")
    public ResponseEntity<AnnotationDTO> createAnnotation(@Valid @RequestBody AnnotationDTO annotationDTO) throws URISyntaxException {
        log.debug("REST request to save Annotation : {}", annotationDTO);
        if (annotationDTO.getId() != null) {
            throw new BadRequestAlertException("A new annotation cannot already have an ID", ENTITY_NAME, "idexists");
        }
        AnnotationDTO result = annotationService.save(annotationDTO);
        return ResponseEntity
            .created(new URI("/api/annotations/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update an existing annotation")
    @ApiResponse(responseCode = "200", description = "Annotation updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<AnnotationDTO> updateAnnotation(
        @Parameter(description = "Annotation ID") @PathVariable(value = "id", name = "id", required = false) final String id,
        @Valid @RequestBody AnnotationDTO annotationDTO) {
        log.debug("REST request to update Annotation : {}, {}", id, annotationDTO);
        if (!Objects.equals(id, annotationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!annotationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        AnnotationDTO result = annotationService.update(annotationDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, annotationDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Partially update an annotation")
    @ApiResponse(responseCode = "200", description = "Annotation partially updated")
    @ApiResponse(responseCode = "404", description = "Annotation not found")
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AnnotationDTO> partialUpdateAnnotation(
        @Parameter(description = "Annotation ID") @PathVariable(value = "id", name = "id", required = false) final String id,
        @NotNull @RequestBody AnnotationDTO annotationDTO) {
        log.debug("REST request to partial update Annotation partially : {}, {}", id, annotationDTO);
        if (!Objects.equals(id, annotationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!annotationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Optional<AnnotationDTO> result = annotationService.partialUpdate(annotationDTO);
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, annotationDTO.getId())
        );
    }

    @Operation(summary = "Get all annotations")
    @ApiResponse(responseCode = "200", description = "List of annotations")
    @GetMapping("")
    public List<AnnotationDTO> getAllAnnotations() {
        log.debug("REST request to get all Annotations");
        return annotationService.findAll();
    }

    @Operation(summary = "Get an annotation by ID")
    @ApiResponse(responseCode = "200", description = "Annotation found")
    @ApiResponse(responseCode = "404", description = "Annotation not found")
    @GetMapping("/{id}")
    public ResponseEntity<AnnotationDTO> getAnnotation(
        @Parameter(description = "Annotation ID") @PathVariable(name = "id") String id) {
        log.debug("REST request to get Annotation : {}", id);
        Optional<AnnotationDTO> annotationDTO = annotationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(annotationDTO);
    }

    @Operation(summary = "Delete an annotation")
    @ApiResponse(responseCode = "204", description = "Annotation deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnotation(
        @Parameter(description = "Annotation ID") @PathVariable(name = "id") String id) {
        log.debug("REST request to delete Annotation : {}", id);
        annotationService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
