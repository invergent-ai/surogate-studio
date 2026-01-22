package net.statemesh.web.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.service.EvaluationJobService;
import net.statemesh.service.dto.EvaluationJobDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class EvaluationJobResource {

    private static final String ENTITY_NAME = "evaluationJob";

    // Proprietățile permise pentru sortare (la fel ca în UserResource)
    private static final List<String> ALLOWED_ORDERED_PROPERTIES = List.of(
        "id", "runName", "baseModel", "status", "language", "judgeModel", "createdDate", "lastModifiedDate"
    );

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final EvaluationJobService evaluationJobService;

    /**
     * {@code POST  /evaluation-jobs} : Create a new evaluation job.
     */
    @PostMapping("/evaluation-jobs")
    public ResponseEntity<EvaluationJobDTO> createEvaluationJob(@Valid @RequestBody EvaluationJobDTO dto)
        throws URISyntaxException {
        log.debug("REST request to save EvaluationJob : {}", dto);

        if (dto.getId() != null) {
            throw new BadRequestAlertException("A new evaluationJob cannot already have an ID", ENTITY_NAME, "idexists");
        }

        EvaluationJobDTO result = evaluationJobService.save(dto);
        return ResponseEntity
            .created(new URI("/api/evaluation-jobs/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    /**
     * {@code PUT  /evaluation-jobs/:id} : Updates an existing EvaluationJob.
     */
    @PutMapping("/evaluation-jobs/{id}")
    public ResponseEntity<EvaluationJobDTO> updateEvaluationJob(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody EvaluationJobDTO dto
    ) {
        log.debug("REST request to update EvaluationJob : {}, {}", id, dto);

        if (dto.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }

        if (!id.equals(dto.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        EvaluationJobDTO result = evaluationJobService.update(dto);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, dto.getId().toString()))
            .body(result);
    }

    /**
     * {@code GET  /evaluation-jobs} : get all the evaluation jobs.
     */
    @GetMapping("/evaluation-jobs")
    public ResponseEntity<List<EvaluationJobDTO>> getAllEvaluationJobs(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable
    ) {
        log.debug("REST request to get all EvaluationJobs");

        if (!onlyContainsAllowedProperties(pageable)) {
            return ResponseEntity.badRequest().build();
        }

        final Page<EvaluationJobDTO> page = evaluationJobService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(),
            page
        );

        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    /**
     * {@code GET  /evaluation-jobs/:id} : get the "id" evaluation job.
     */
    @GetMapping("/evaluation-jobs/{id}")
    public ResponseEntity<EvaluationJobDTO> getEvaluationJob(@PathVariable("id") Long id) {
        log.debug("REST request to get EvaluationJob : {}", id);
        Optional<EvaluationJobDTO> dto = evaluationJobService.findOne(id);
        return ResponseUtil.wrapOrNotFound(dto);
    }

    /**
     * {@code DELETE  /evaluation-jobs/:id} : delete the "id" evaluation job.
     */
    @DeleteMapping("/evaluation-jobs/{id}")
    public ResponseEntity<Void> deleteEvaluationJob(@PathVariable Long id) {
        log.debug("REST request to delete EvaluationJob : {}", id);
        evaluationJobService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
    }

    // -- Utility methods -------------------------------------------------------

    private boolean onlyContainsAllowedProperties(Pageable pageable) {
        return pageable.getSort()
            .stream()
            .map(Sort.Order::getProperty)
            .allMatch(ALLOWED_ORDERED_PROPERTIES::contains);
    }
}
