package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import net.logstash.logback.util.StringUtils;
import net.statemesh.service.NodeReservationService;
import net.statemesh.service.dto.NodeReservationDTO;
import net.statemesh.service.exception.BadRequestAlertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.jhipster.web.util.HeaderUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.List;

import static net.statemesh.config.Constants.SM_ID_HEADER;

@RestController
@RequestMapping("/api/node-reservation")
@Tag(name = "Node Reservations", description = "Node reservation management")
public class NodeReservationResource {
    private final Logger log = LoggerFactory.getLogger(NodeReservationResource.class);
    private static final String ENTITY_NAME = "nodeReservation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final NodeReservationService nodeReservationService;

    public NodeReservationResource(NodeReservationService nodeReservationService) {
        this.nodeReservationService = nodeReservationService;
    }

    @Operation(summary = "Get or create a node reservation")
    @ApiResponse(responseCode = "201", description = "Reservation returned or created")
    @GetMapping("")
    public ResponseEntity<NodeReservationDTO> getOrCreateNodeReservation(Principal principal)
        throws URISyntaxException {
        log.debug("REST request to get or create NodeReservation : {}", principal.getName());
        NodeReservationDTO result = nodeReservationService.getOrCreate(principal.getName());
        return ResponseEntity
            .created(new URI("/api/node-reservation/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId()))
            .body(result);
    }

    @Operation(summary = "List user's node reservations")
    @ApiResponse(responseCode = "200", description = "Reservations returned")
    @GetMapping("/query")
    public ResponseEntity<List<NodeReservationDTO>> queryNodeReservations(Principal principal) {
        log.debug("REST request to list NodeReservation : {}", principal.getName());
        return ResponseEntity.ok(nodeReservationService.findReservationsforUser(principal.getName()));
    }

    @Operation(summary = "Add error to a node reservation")
    @ApiResponse(responseCode = "200", description = "Error added")
    @ApiResponse(responseCode = "400", description = "Missing or invalid SM ID header")
    @PutMapping(value = "/error", consumes = "text/plain")
    public ResponseEntity<Void> addNodeReservationError(
        @Parameter(description = "Short SM ID header") @RequestHeader(SM_ID_HEADER) String shortSmId,
        @RequestBody String error
    ) {
        if (StringUtils.isEmpty(shortSmId)) {
            throw new BadRequestAlertException(SM_ID_HEADER + " is missing", SM_ID_HEADER, "missing");
        }
        var reservationDTO = nodeReservationService.getForShortSmId(shortSmId)
            .orElseThrow(() -> new BadRequestAlertException(SM_ID_HEADER + " is invalid", SM_ID_HEADER, "invalid"));
        nodeReservationService.addError(reservationDTO.getId(), error);
        return ResponseEntity.ok().build();
    }
}
