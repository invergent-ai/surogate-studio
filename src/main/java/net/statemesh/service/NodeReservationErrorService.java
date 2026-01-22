package net.statemesh.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import net.statemesh.domain.NodeReservationError;
import net.statemesh.repository.NodeReservationErrorRepository;
import net.statemesh.repository.NodeReservationRepository;
import net.statemesh.service.dto.NodeReservationDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.NodeReservationErrorMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class NodeReservationErrorService {
    private final NodeReservationErrorRepository nodeReservationErrorRepository;
    private final NodeReservationErrorMapper nodeReservationErrorMapper;
    private final ObjectMapper objectMapper;
    final NodeReservationRepository nodeReservationRepository;

    public void createWithLogs(String logs, NodeReservationDTO nodeReservationDTO) {
        var reservation = nodeReservationRepository.findById(nodeReservationDTO.getId()).orElseThrow();
        nodeReservationErrorMapper.toDto(
            nodeReservationErrorRepository.save(
                NodeReservationError.builder()
                    .created(Instant.now())
                    .nodeReservation(reservation)
                    .error(logs)
                    .build()
            ),
            new CycleAvoidingMappingContext()
        );
    }

    public void createWithErrors(List<OnboardingErrorMessage> errors, NodeReservationDTO nodeReservationDTO) {
        try {
            String json = objectMapper.writerFor(new TypeReference<List<OnboardingErrorMessage>>() {}).writeValueAsString(errors);
            var reservation = nodeReservationRepository.findById(nodeReservationDTO.getId()).orElseThrow();
            nodeReservationErrorMapper.toDto(
                nodeReservationErrorRepository.save(
                    NodeReservationError.builder()
                        .created(Instant.now())
                        .nodeReservation(reservation)
                        .errors(json)
                        .build()
                ),
                new CycleAvoidingMappingContext()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static record OnboardingErrorMessage(String error) {}
}
