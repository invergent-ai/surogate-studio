package net.statemesh.web.rest;

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

/**
 * REST controller for managing {@link net.statemesh.domain.NodeReservation}.
 */
@RestController
@RequestMapping("/api/node-reservation")
public class NodeReservationResource {
    private final Logger log = LoggerFactory.getLogger(NodeReservationResource.class);
    private static final String ENTITY_NAME = "nodeReservation";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final NodeReservationService nodeReservationService;

    public NodeReservationResource(NodeReservationService nodeReservationService) {
        this.nodeReservationService = nodeReservationService;
    }

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

    @GetMapping("/query")
    public ResponseEntity<List<NodeReservationDTO>> queryNodeReservations(Principal principal) {
        log.debug("REST request to list NodeReservation : {}", principal.getName());
        return ResponseEntity.ok(nodeReservationService.findReservationsforUser(principal.getName()));
    }

    @PutMapping(value = "/error", consumes = "text/plain")
    public ResponseEntity<Void> addNodeReservationError(
        @RequestHeader(SM_ID_HEADER) String shortSmId,
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
