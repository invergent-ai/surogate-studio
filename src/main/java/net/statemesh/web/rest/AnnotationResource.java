package net.statemesh.web.rest;

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

/**
 * REST controller for managing {@link net.statemesh.domain.Annotation}.
 */
@RestController
@RequestMapping("/api/annotations")
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

    /**
     * {@code POST  /annotations} : Create a new annotation.
     *
     * @param annotationDTO the annotationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new annotationDTO, or with status {@code 400 (Bad Request)} if the annotation has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
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

    /**
     * {@code PUT  /annotations/:id} : Updates an existing annotation.
     *
     * @param id the id of the annotationDTO to save.
     * @param annotationDTO the annotationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated annotationDTO,
     * or with status {@code 400 (Bad Request)} if the annotationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the annotationDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AnnotationDTO> updateAnnotation(
        @PathVariable(value = "id", name = "id", required = false) final String id,
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

    /**
     * {@code PATCH  /annotations/:id} : Partial updates given fields of an existing annotation, field will ignore if it is null
     *
     * @param id the id of the annotationDTO to save.
     * @param annotationDTO the annotationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated annotationDTO,
     * or with status {@code 400 (Bad Request)} if the annotationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the annotationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the annotationDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<AnnotationDTO> partialUpdateAnnotation(
        @PathVariable(value = "id", name = "id", required = false) final String id,
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

    /**
     * {@code GET  /annotations} : get all the annotations.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of annotations in body.
     */
    @GetMapping("")
    public List<AnnotationDTO> getAllAnnotations() {
        log.debug("REST request to get all Annotations");
        return annotationService.findAll();
    }

    /**
     * {@code GET  /annotations/:id} : get the "id" annotation.
     *
     * @param id the id of the annotationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the annotationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AnnotationDTO> getAnnotation(@PathVariable(name = "id") String id) {
        log.debug("REST request to get Annotation : {}", id);
        Optional<AnnotationDTO> annotationDTO = annotationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(annotationDTO);
    }

    /**
     * {@code DELETE  /annotations/:id} : delete the "id" annotation.
     *
     * @param id the id of the annotationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnotation(@PathVariable(name = "id") String id) {
        log.debug("REST request to delete Annotation : {}", id);
        annotationService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
