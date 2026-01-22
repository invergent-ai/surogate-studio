package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import net.statemesh.domain.Provider;
import net.statemesh.repository.ProviderRepository;
import net.statemesh.service.dto.ProviderDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.ProviderMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProviderService {
    private final Logger log = LoggerFactory.getLogger(ProviderService.class);

    private final ProviderRepository providerRepository;
    private final ProviderMapper providerMapper;

    @Transactional(readOnly = true)
    public List<ProviderDTO> findAllActive() {
        log.debug("Request to get all active providers");
        return providerRepository.findActiveProviders().stream()
            .map(p -> providerMapper.toDto(p, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }
}
