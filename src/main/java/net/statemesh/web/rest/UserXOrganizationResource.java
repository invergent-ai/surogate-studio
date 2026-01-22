package net.statemesh.web.rest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.statemesh.repository.UserXOrganizationRepository;
import net.statemesh.service.UserXOrganizationService;
import net.statemesh.service.dto.UserXOrganizationDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link net.statemesh.domain.UserXOrganization}.
 */
@RestController
@RequestMapping("/api/user-x-organizations")
public class UserXOrganizationResource {
    private final Logger log = LoggerFactory.getLogger(UserXOrganizationResource.class);

    private static final String ENTITY_NAME = "userXOrganization";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final UserXOrganizationService userXOrganizationService;

    private final UserXOrganizationRepository userXOrganizationRepository;

    public UserXOrganizationResource(
        UserXOrganizationService userXOrganizationService,
        UserXOrganizationRepository userXOrganizationRepository
    ) {
        this.userXOrganizationService = userXOrganizationService;
        this.userXOrganizationRepository = userXOrganizationRepository;
    }

    /**
     * {@code POST  /user-x-organizations} : Create a new userXOrganization.
     *
     * @param userXOrganizationDTO the userXOrganizationDTO to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new userXOrganizationDTO, or with status {@code 400 (Bad Request)} if the userXOrganization has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public ResponseEntity<UserXOrganizationDTO> createUserXOrganization(@Valid @RequestBody UserXOrganizationDTO userXOrganizationDTO)
        throws URISyntaxException {
        log.debug("REST request to save UserXOrganization : {}", userXOrganizationDTO);
        if (userXOrganizationDTO.getId() != null) {
            throw new BadRequestAlertException("A new userXOrganization cannot already have an ID", ENTITY_NAME, "idexists");
        }
        UserXOrganizationDTO result = userXOrganizationService.save(userXOrganizationDTO);
        return ResponseEntity
            .created(new URI("/api/user-x-organizations/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    /**
     * {@code PUT  /user-x-organizations/:id} : Updates an existing userXOrganization.
     *
     * @param id the id of the userXOrganizationDTO to save.
     * @param userXOrganizationDTO the userXOrganizationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userXOrganizationDTO,
     * or with status {@code 400 (Bad Request)} if the userXOrganizationDTO is not valid,
     * or with status {@code 500 (Internal Server Error)} if the userXOrganizationDTO couldn't be updated.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserXOrganizationDTO> updateUserXOrganization(
        @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody UserXOrganizationDTO userXOrganizationDTO) {
        log.debug("REST request to update UserXOrganization : {}, {}", id, userXOrganizationDTO);
        if (!Objects.equals(id, userXOrganizationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userXOrganizationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        UserXOrganizationDTO result = userXOrganizationService.update(userXOrganizationDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userXOrganizationDTO.getId()))
            .body(result);
    }

    /**
     * {@code PATCH  /user-x-organizations/:id} : Partial updates given fields of an existing userXOrganization, field will ignore if it is null
     *
     * @param id the id of the userXOrganizationDTO to save.
     * @param userXOrganizationDTO the userXOrganizationDTO to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated userXOrganizationDTO,
     * or with status {@code 400 (Bad Request)} if the userXOrganizationDTO is not valid,
     * or with status {@code 404 (Not Found)} if the userXOrganizationDTO is not found,
     * or with status {@code 500 (Internal Server Error)} if the userXOrganizationDTO couldn't be updated.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public ResponseEntity<UserXOrganizationDTO> partialUpdateUserXOrganization(
        @PathVariable(value = "id", required = false) final String id,
        @NotNull @RequestBody UserXOrganizationDTO userXOrganizationDTO) {
        log.debug("REST request to partial update UserXOrganization partially : {}, {}", id, userXOrganizationDTO);
        if (!Objects.equals(id, userXOrganizationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!userXOrganizationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<UserXOrganizationDTO> result = userXOrganizationService.partialUpdate(userXOrganizationDTO);

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, userXOrganizationDTO.getId())
        );
    }

    /**
     * {@code GET  /user-x-organizations} : get all the userXOrganizations.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of userXOrganizations in body.
     */
    @GetMapping("")
    public List<UserXOrganizationDTO> getAllUserXOrganizations() {
        log.debug("REST request to get all UserXOrganizations");
        return userXOrganizationService.findAll();
    }

    /**
     * {@code GET  /user-x-organizations/:id} : get the "id" userXOrganization.
     *
     * @param id the id of the userXOrganizationDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the userXOrganizationDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserXOrganizationDTO> getUserXOrganization(@PathVariable String id) {
        log.debug("REST request to get UserXOrganization : {}", id);
        Optional<UserXOrganizationDTO> userXOrganizationDTO = userXOrganizationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(userXOrganizationDTO);
    }

    /**
     * {@code DELETE  /user-x-organizations/:id} : delete the "id" userXOrganization.
     *
     * @param id the id of the userXOrganizationDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUserXOrganization(@PathVariable String id) {
        log.debug("REST request to delete UserXOrganization : {}", id);
        userXOrganizationService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
