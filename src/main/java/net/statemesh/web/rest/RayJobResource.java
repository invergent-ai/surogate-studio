package net.statemesh.web.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.service.RayJobService;
import net.statemesh.service.criteria.RayJobCriteria;
import net.statemesh.service.dto.RayJobDTO;
import net.statemesh.service.query.RayJobQueryService;
import org.springdoc.core.annotations.ParameterObject;
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
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Slf4j
public class RayJobResource {
    private static final String ENTITY_NAME = "rayJob";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final RayJobService rayJobService;
    private final RayJobQueryService rayJobQueryService;


    @PostMapping("")
    public ResponseEntity<RayJobDTO> saveRayJob(@RequestBody RayJobDTO rayJobDTO,
                                                Principal principal) throws URISyntaxException {
        log.debug("REST request to save RayJob : {}", rayJobDTO);
        rayJobService.initNames(rayJobDTO);
        rayJobService.loadInternalsAndDumpTrainingConfig(rayJobDTO);
        RayJobDTO result = rayJobService.save(rayJobDTO, principal.getName());
        rayJobService.restoreTransient(rayJobDTO, result);
        return ResponseEntity
            .created(new URI("/api/jobs/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @PostMapping("/deploy")
    public ResponseEntity<RayJobDTO> deployRayJob(@RequestBody RayJobDTO rayJobDTO,
                                                  Principal principal) throws URISyntaxException {
        log.debug("REST request to deploy RayJob : {}", rayJobDTO);
        RayJobDTO result = rayJobService.deploy(rayJobDTO, principal.getName());

        return ResponseEntity
            .created(new URI("/api/jobs/deploy" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @PostMapping("/redeploy")
    public ResponseEntity<RayJobDTO> redeployRayJob(@RequestBody RayJobDTO rayJobDTO,
                                                    Principal principal) throws URISyntaxException {
        log.debug("REST request to redeploy RayJob : {}", rayJobDTO);
        RayJobDTO result = rayJobService.redeploy(rayJobDTO, principal.getName());

        return ResponseEntity
            .created(new URI("/api/jobs/redeploy" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @DeleteMapping("/cancel/{id}")
    public ResponseEntity<Void> cancelRayJob(@PathVariable(name = "id") String id,
                                             Principal principal) {
        log.debug("REST request to cancel RayJob : {}", id);
        rayJobService.cancel(id, principal.getName());
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteRayJob(@PathVariable(name = "id") String id,
                                             Principal principal) {
        log.debug("REST request to delete RayJob : {}", id);
        rayJobService.delete(id, principal.getName());
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<RayJobDTO> getRayJob(@PathVariable(name = "id") String id) {
        log.debug("REST request to get RayJob : {}", id);
        Optional<RayJobDTO> rayJobDTO = rayJobService.findOne(id);
        rayJobService.loadTrainingConfig(rayJobDTO);
        return ResponseUtil.wrapOrNotFound(rayJobDTO);
    }

    @GetMapping("")
    public ResponseEntity<List<RayJobDTO>> queryRayJobs(
        RayJobCriteria criteria,
        @ParameterObject Pageable pageable,
        Principal principal
    ) {
        log.debug("REST request to get RayJobs by criteria: {}", criteria);
        Page<RayJobDTO> resultPage = rayJobQueryService.findByCriteria(criteria, pageable, principal.getName());
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), resultPage);
        return ResponseEntity.ok().headers(headers).body(resultPage.getContent());
    }
}
