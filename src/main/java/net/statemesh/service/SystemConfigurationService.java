package net.statemesh.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.statemesh.domain.SystemConfiguration;
import net.statemesh.repository.SystemConfigurationRepository;
import net.statemesh.service.dto.SystemConfigurationDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.SystemConfigurationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link net.statemesh.domain.SystemConfiguration}.
 */
@Service
@Transactional
@RequiredArgsConstructor
@DependsOnDatabaseInitialization
public class SystemConfigurationService {
    private final Logger log = LoggerFactory.getLogger(SystemConfigurationService.class);

    private final SystemConfigurationRepository systemConfigurationRepository;
    private final SystemConfigurationMapper systemConfigurationMapper;
    @Getter
    private SystemConfigurationDTO config;

    @PostConstruct
    void initialize() {
        this.config = findAll().stream()
            .findFirst()
            .orElseThrow(() -> new RuntimeException("System configuration is missing"));
    }

    /**
     * Save a systemConfiguration.
     *
     * @param systemConfigurationDTO the entity to save.
     * @return the persisted entity.
     */
    public SystemConfigurationDTO save(SystemConfigurationDTO systemConfigurationDTO) {
        log.debug("Request to save SystemConfiguration : {}", systemConfigurationDTO);
        SystemConfiguration systemConfiguration = systemConfigurationMapper.toEntity(systemConfigurationDTO, new CycleAvoidingMappingContext());
        systemConfiguration = systemConfigurationRepository.save(systemConfiguration);
        return systemConfigurationMapper.toDto(systemConfiguration, new CycleAvoidingMappingContext());
    }

    /**
     * Update a systemConfiguration.
     *
     * @param systemConfigurationDTO the entity to save.
     * @return the persisted entity.
     */
    public SystemConfigurationDTO update(SystemConfigurationDTO systemConfigurationDTO) {
        log.debug("Request to update SystemConfiguration : {}", systemConfigurationDTO);
        SystemConfiguration systemConfiguration = systemConfigurationMapper.toEntity(systemConfigurationDTO, new CycleAvoidingMappingContext());
        systemConfiguration = systemConfigurationRepository.save(systemConfiguration);
        return systemConfigurationMapper.toDto(systemConfiguration, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a systemConfiguration.
     *
     * @param systemConfigurationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<SystemConfigurationDTO> partialUpdate(SystemConfigurationDTO systemConfigurationDTO) {
        log.debug("Request to partially update SystemConfiguration : {}", systemConfigurationDTO);

        return systemConfigurationRepository
            .findById(systemConfigurationDTO.getId())
            .map(existingSystemConfiguration -> {
                systemConfigurationMapper.partialUpdate(existingSystemConfiguration, systemConfigurationDTO);

                return existingSystemConfiguration;
            })
            .map(systemConfigurationRepository::save)
            .map(o -> systemConfigurationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the systemConfigurations.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<SystemConfigurationDTO> findAll() {
        log.debug("Request to get all SystemConfigurations");
        return systemConfigurationRepository
            .findAll()
            .stream()
            .map(o -> systemConfigurationMapper.toDto(o, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one systemConfiguration by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<SystemConfigurationDTO> findOne(String id) {
        log.debug("Request to get SystemConfiguration : {}", id);
        return systemConfigurationRepository.findById(id)
            .map(o -> systemConfigurationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the systemConfiguration by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete SystemConfiguration : {}", id);
        systemConfigurationRepository.deleteById(id);
    }
}
