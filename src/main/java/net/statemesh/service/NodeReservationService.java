package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.NodeReservation;
import net.statemesh.domain.NodeReservationError;
import net.statemesh.domain.User;
import net.statemesh.k8s.util.NamingUtils;
import net.statemesh.repository.*;
import net.statemesh.service.dto.NodeBenchmarkDTO;
import net.statemesh.service.dto.NodeReservationDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.NodeBenchmarkMapper;
import net.statemesh.service.mapper.NodeReservationMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static net.statemesh.config.Constants.NODE_BASE_NAME;
import static net.statemesh.security.SecurityUtils.*;

/**
 * Service Implementation for managing {@link net.statemesh.domain.NodeReservation}.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NodeReservationService {
    private final NodeReservationRepository nodeReservationRepository;
    private final NodeBenchmarkRepository nodeBenchmarkRepository;
    private final NodeReservationErrorRepository nodeReservationErrorRepository;
    private final NodeReservationMapper nodeReservationMapper;
    private final NodeBenchmarkMapper nodeBenchmarkMapper;
    private final UserRepository userRepository;
    private final NodeRepository nodeRepository;
    private final NodeHistoryService nodeHistoryService;
    private final JwtEncoder jwtEncoder;
    private final ApplicationProperties applicationProperties;

    public NodeReservationDTO getOrCreate(String login) {
        log.debug("Request to get or create NodeReservation for user: {}", login);
        Optional<NodeReservation> nodeReservation =
            nodeReservationRepository.findActiveReservationForUser(login, Instant.now());
        if (nodeReservation.isPresent()) {
            log.debug("Returning existing reservation for user {}", login);
            return nodeReservationMapper.toDto(nodeReservation.get(), new CycleAvoidingMappingContext());
        }

        final User user = userRepository
            .findOneByLoginIgnoreCase(login)
            .orElseThrow(() -> new RuntimeException(login + " wasn't found!"));
        log.debug("Creating new node reservation for user {}", login);

        String token = createToken(login);

        return nodeReservationMapper.toDto(
            nodeReservationRepository.save(
                NodeReservation.builder()
                    .internalName(generateInternalName(user))
                    .created(Instant.now())
                    .updated(Instant.now())
                    .expireTime(
                        Instant.now().plus(applicationProperties.getNodeReservationDuration(), ChronoUnit.MINUTES)
                    )
                    .smId(token)
                    .shortSmId(shortSmId())
                    .user(user)
                    .build()
            ), new CycleAvoidingMappingContext()
        );
    }

    public Optional<NodeReservationDTO> getForShortSmId(String smId) {
        log.debug("Request to get a NodeReservation for shortSmId: {}", smId);
        Optional<NodeReservation> reservation =
            nodeReservationRepository.findActiveReservationForShortSmId(smId);
        return reservation.map(o -> nodeReservationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional
    public NodeReservationDTO save(NodeReservationDTO nodeReservationDTO) {
        log.debug("Request to save NodeReservation : {}", nodeReservationDTO);
        NodeReservation nodeReservation = nodeReservationMapper.toEntity(nodeReservationDTO, new CycleAvoidingMappingContext());
        nodeReservation = nodeReservationRepository.save(nodeReservation);
        return nodeReservationMapper.toDto(nodeReservation, new CycleAvoidingMappingContext());
    }

    public Optional<NodeReservationDTO> partialUpdate(NodeReservationDTO dto) {
        log.debug("Request to partially update NodeReservation : {}", dto);

        return nodeReservationRepository
            .findById(dto.getId())
            .map(existingPort -> {
                nodeReservationMapper.partialUpdate(existingPort, dto);
                return existingPort;
            })
            .map(nodeReservationRepository::save)
            .map(o -> nodeReservationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public Optional<NodeReservationDTO> findActiveReservationForUserKey(String userKey, Instant when) {
        log.debug("Request to get active reservation for userKey: {}", userKey);
        return nodeReservationRepository.findActiveReservationForUserKey(userKey, when)
            .map(o -> nodeReservationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public Optional<NodeReservationDTO> findReservationForUserKey(String userKey) {
        log.debug("Request to get reservation for userKey: {}", userKey);
        return nodeReservationRepository.findReservationForUserKey(userKey)
            .map(o -> nodeReservationMapper.toDto(o, new CycleAvoidingMappingContext()))
            .map(this::addNodeHistory);
    }

    @Transactional(readOnly = true)
    public List<NodeReservationDTO> findReservationsforUser(String login) {
        return nodeReservationRepository.findAllForUser(login).stream()
            .map(o -> nodeReservationMapper.toDto(o, new CycleAvoidingMappingContext()))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<NodeBenchmarkDTO> findLatestBenchmark(NodeReservationDTO nodeReservationDTO) {
        return nodeBenchmarkRepository.findLatestByShortSmId(nodeReservationDTO.getShortSmId())
            .map(o -> nodeBenchmarkMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional
    public void addError(String reservationId, String error) {
        Optional<NodeReservation> reservation = nodeReservationRepository.findById(reservationId);
        if (reservation.isEmpty()) {
            return;
        }

        nodeReservationErrorRepository.save(
            NodeReservationError.builder()
                .nodeReservation(reservation.get())
                .error(error)
                .created(Instant.now())
                .build()
        );
    }

    private NodeReservationDTO addNodeHistory(NodeReservationDTO nodeReservationDTO) {
        if (nodeReservationDTO.getNode() != null) {
            nodeReservationDTO.getNode().setHistory(
                new HashSet<>(nodeHistoryService.findForNode(nodeReservationDTO.getNode()))
            );
        }

        return nodeReservationDTO;
    }

    private String createToken(String login) {
        return this.jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(JWT_ALGORITHM).build(),
                    JwtClaimsSet.builder()
                        .issuedAt(Instant.now())
                        .expiresAt(
                            Instant.now().plus(applicationProperties.getNodeReservationDuration(), ChronoUnit.MINUTES)
                        )
                        .subject(login)
                        .claim(AUTHORITIES_KEY, Strings.EMPTY)
                        .claim(USER_KEY, NamingUtils.rfc1123Name(RandomStringUtils.secure().nextAlphanumeric(USER_KEY_LENGTH)))
                        .build()
                )
            )
            .getTokenValue();
    }

    protected static String shortSmId() {
        UUID uuid = UUID.randomUUID();
        long lo = uuid.getLeastSignificantBits();
        long hi = uuid.getMostSignificantBits();
        lo = (lo >> (64 - 31)) ^ lo;
        hi = (hi >> (64 - 31)) ^ hi;
        return String.format("%010d", Math.abs(Math.abs(hi) + Math.abs(lo)));
    }

    private String generateInternalName(User user) {
        long current = nodeRepository.countAllByUser_Id(user.getId()) + 1;
        long currentReservations = nodeReservationRepository.count() + 1;

        return NODE_BASE_NAME + current + "-" + currentReservations + "-" +
            RandomStringUtils.secure().nextAlphanumeric(10).toLowerCase();
    }
}
