package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@RestController
@RequestMapping("/api/user-x-organizations")
@Tag(name = "User Organization", description = "User-organization association management")
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

    @Operation(summary = "Create a new user-organization association")
    @ApiResponse(responseCode = "201", description = "Association created")
    @ApiResponse(responseCode = "400", description = "Invalid input or ID already exists")
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

    @Operation(summary = "Update an existing user-organization association")
    @ApiResponse(responseCode = "200", description = "Association updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
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

    @Operation(summary = "Partially update a user-organization association")
    @ApiResponse(responseCode = "200", description = "Association partially updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID or input")
    @ApiResponse(responseCode = "404", description = "Association not found")
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

    @Operation(summary = "Get all user-organization associations")
    @ApiResponse(responseCode = "200", description = "List of associations returned")
    @GetMapping("")
    public List<UserXOrganizationDTO> getAllUserXOrganizations() {
        log.debug("REST request to get all UserXOrganizations");
        return userXOrganizationService.findAll();
    }

    @Operation(summary = "Get a user-organization association by ID")
    @ApiResponse(responseCode = "200", description = "Association returned")
    @ApiResponse(responseCode = "404", description = "Association not found")
    @GetMapping("/{id}")
    public ResponseEntity<UserXOrganizationDTO> getUserXOrganization(@PathVariable String id) {
        log.debug("REST request to get UserXOrganization : {}", id);
        Optional<UserXOrganizationDTO> userXOrganizationDTO = userXOrganizationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(userXOrganizationDTO);
    }

    @Operation(summary = "Delete a user-organization association")
    @ApiResponse(responseCode = "204", description = "Association deleted")
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
