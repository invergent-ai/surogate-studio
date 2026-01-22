package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import net.statemesh.domain.EnvironmentVariable;
import net.statemesh.repository.EnvironmentVariableRepository;
import net.statemesh.service.dto.EnvironmentVariableDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.EnvironmentVariableMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link net.statemesh.domain.EnvironmentVariable}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class EnvironmentVariableService {
    private final Logger log = LoggerFactory.getLogger(EnvironmentVariableService.class);

    private final EnvironmentVariableRepository environmentVariableRepository;
    private final EnvironmentVariableMapper environmentVariableMapper;

    /**
     * Save a environmentVariable.
     *
     * @param environmentVariableDTO the entity to save.
     * @return the persisted entity.
     */
    public EnvironmentVariableDTO save(EnvironmentVariableDTO environmentVariableDTO) {
        log.debug("Request to save EnvironmentVariable : {}", environmentVariableDTO);
        EnvironmentVariable environmentVariable = environmentVariableMapper.toEntity(environmentVariableDTO, new CycleAvoidingMappingContext());
        environmentVariable = environmentVariableRepository.save(environmentVariable);
        return environmentVariableMapper.toDto(environmentVariable, new CycleAvoidingMappingContext());
    }

    /**
     * Update a environmentVariable.
     *
     * @param environmentVariableDTO the entity to save.
     * @return the persisted entity.
     */
    public EnvironmentVariableDTO update(EnvironmentVariableDTO environmentVariableDTO) {
        log.debug("Request to update EnvironmentVariable : {}", environmentVariableDTO);
        EnvironmentVariable environmentVariable = environmentVariableMapper.toEntity(environmentVariableDTO, new CycleAvoidingMappingContext());
        environmentVariable = environmentVariableRepository.save(environmentVariable);
        return environmentVariableMapper.toDto(environmentVariable, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a environmentVariable.
     *
     * @param environmentVariableDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<EnvironmentVariableDTO> partialUpdate(EnvironmentVariableDTO environmentVariableDTO) {
        log.debug("Request to partially update EnvironmentVariable : {}", environmentVariableDTO);

        return environmentVariableRepository
            .findById(environmentVariableDTO.getId())
            .map(existingEnvironmentVariable -> {
                environmentVariableMapper.partialUpdate(existingEnvironmentVariable, environmentVariableDTO);

                return existingEnvironmentVariable;
            })
            .map(environmentVariableRepository::save)
            .map(o -> environmentVariableMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the environmentVariables.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<EnvironmentVariableDTO> findAll() {
        log.debug("Request to get all EnvironmentVariables");
        return environmentVariableRepository
            .findAll()
            .stream()
            .map(o -> environmentVariableMapper.toDto(o, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one environmentVariable by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<EnvironmentVariableDTO> findOne(String id) {
        log.debug("Request to get EnvironmentVariable : {}", id);
        return environmentVariableRepository.findById(id).map(o -> environmentVariableMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the environmentVariable by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete EnvironmentVariable : {}", id);
        environmentVariableRepository.deleteById(id);
    }
}
