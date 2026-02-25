package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import net.statemesh.repository.NotificationRepository;
import net.statemesh.security.SecurityUtils;
import net.statemesh.service.NotificationService;
import net.statemesh.service.dto.NotificationDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "User notification management")
public class NotificationResource {

    private final Logger log = LoggerFactory.getLogger(NotificationResource.class);
    private static final String ENTITY_NAME = "notification";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public NotificationResource(NotificationService notificationService, NotificationRepository notificationRepository) {
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    @Operation(summary = "Create a notification")
    @ApiResponse(responseCode = "201", description = "Notification created")
    @ApiResponse(responseCode = "400", description = "Already has an ID")
    @PostMapping("")
    public ResponseEntity<NotificationDTO> createNotification(@Valid @RequestBody NotificationDTO notificationDTO)
        throws URISyntaxException {
        log.debug("REST request to save Notification : {}", notificationDTO);
        if (notificationDTO.getId() != null) {
            throw new BadRequestAlertException("A new notification cannot already have an ID", ENTITY_NAME, "idexists");
        }
        NotificationDTO result = notificationService.save(notificationDTO);
        return ResponseEntity
            .created(new URI("/api/notifications/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "Update a notification")
    @ApiResponse(responseCode = "200", description = "Notification updated")
    @ApiResponse(responseCode = "400", description = "Invalid ID")
    @PutMapping("/{id}")
    public ResponseEntity<NotificationDTO> updateNotification(
        @Parameter(description = "Notification ID") @PathVariable(value = "id", required = false) final String id,
        @Valid @RequestBody NotificationDTO notificationDTO) {
        log.debug("REST request to update Notification : {}, {}", id, notificationDTO);
        if (!Objects.equals(id, notificationDTO.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }
        if (!notificationRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        NotificationDTO result = notificationService.update(notificationDTO);
        return ResponseEntity
            .ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, notificationDTO.getId()))
            .body(result);
    }

    @Operation(summary = "Get all notifications (paginated)")
    @ApiResponse(responseCode = "200", description = "Paginated notifications returned")
    @GetMapping("")
    public ResponseEntity<List<NotificationDTO>> getAllNotifications(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        log.debug("REST request to get a page of Notifications");
        Page<NotificationDTO> page = notificationService.findAll(pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @Operation(summary = "Get current user's notifications")
    @ApiResponse(responseCode = "200", description = "User notifications returned")
    @GetMapping("/user")
    public ResponseEntity<List<NotificationDTO>> getUserNotifications(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
        Page<NotificationDTO> page = notificationService.findByUserLogin(
            SecurityUtils.getCurrentUserLogin().orElseThrow(), pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(
            ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @Operation(summary = "Mark all notifications as read")
    @ApiResponse(responseCode = "200", description = "All notifications marked as read")
    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead(SecurityUtils.getCurrentUserLogin().orElseThrow());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Mark a notification as read")
    @ApiResponse(responseCode = "200", description = "Notification marked as read")
    @PutMapping("/{id}/mark-read")
    public ResponseEntity<Void> markAsRead(
        @Parameter(description = "Notification ID") @PathVariable("id") String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get a notification by ID")
    @ApiResponse(responseCode = "200", description = "Notification found")
    @ApiResponse(responseCode = "404", description = "Not found")
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDTO> getNotification(
        @Parameter(description = "Notification ID") @PathVariable String id) {
        log.debug("REST request to get Notification : {}", id);
        Optional<NotificationDTO> notificationDTO = notificationService.findOne(id);
        return ResponseUtil.wrapOrNotFound(notificationDTO);
    }

    @Operation(summary = "Delete a notification")
    @ApiResponse(responseCode = "204", description = "Notification deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
        @Parameter(description = "Notification ID") @PathVariable String id) {
        log.debug("REST request to delete Notification : {}", id);
        notificationService.delete(id);
        return ResponseEntity
            .noContent()
            .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id))
            .build();
    }
}
