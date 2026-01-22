package net.statemesh.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.statemesh.domain.Port;
import net.statemesh.repository.PortRepository;
import net.statemesh.service.dto.PortDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.PortMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Port}.
 */
@Service
@Transactional
public class PortService {
    private final Logger log = LoggerFactory.getLogger(PortService.class);

    private final PortRepository portRepository;
    private final PortMapper portMapper;

    public PortService(PortRepository portRepository, PortMapper portMapper) {
        this.portRepository = portRepository;
        this.portMapper = portMapper;
    }

    /**
     * Save a port.
     *
     * @param portDTO the entity to save.
     * @return the persisted entity.
     */
    public PortDTO save(PortDTO portDTO) {
        log.debug("Request to save Port : {}", portDTO);
        Port port = portMapper.toEntity(portDTO, new CycleAvoidingMappingContext());
        port = portRepository.save(port);
        return portMapper.toDto(port, new CycleAvoidingMappingContext());
    }

    /**
     * Update a port.
     *
     * @param portDTO the entity to save.
     * @return the persisted entity.
     */
    public PortDTO update(PortDTO portDTO) {
        log.debug("Request to update Port : {}", portDTO);
        Port port = portMapper.toEntity(portDTO, new CycleAvoidingMappingContext());
        port = portRepository.save(port);
        return portMapper.toDto(port, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a port.
     *
     * @param portDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<PortDTO> partialUpdate(PortDTO portDTO) {
        log.debug("Request to partially update Port : {}", portDTO);

        return portRepository
            .findById(portDTO.getId())
            .map(existingPort -> {
                portMapper.partialUpdate(existingPort, portDTO);

                return existingPort;
            })
            .map(portRepository::save)
            .map(o -> portMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the ports.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<PortDTO> findAll() {
        log.debug("Request to get all Ports");
        return portRepository.findAll().stream().map(o -> portMapper.toDto(o, new CycleAvoidingMappingContext())).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the ports with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<PortDTO> findAllWithEagerRelationships(Pageable pageable) {
        return portRepository.findAllWithEagerRelationships(pageable).map(o -> portMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get one port by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<PortDTO> findOne(String id) {
        log.debug("Request to get Port : {}", id);
        return portRepository.findOneWithEagerRelationships(id).map(o -> portMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the port by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete Port : {}", id);
        portRepository.deleteById(id);
    }
}
