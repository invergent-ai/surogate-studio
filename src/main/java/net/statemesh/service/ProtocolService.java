package net.statemesh.service;

import java.util.Optional;
import net.statemesh.domain.Protocol;
import net.statemesh.repository.ProtocolRepository;
import net.statemesh.service.dto.ProtocolDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.ProtocolMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Protocol}.
 */
@Service
@Transactional
public class ProtocolService {
    private final Logger log = LoggerFactory.getLogger(ProtocolService.class);

    private final ProtocolRepository protocolRepository;
    private final ProtocolMapper protocolMapper;

    public ProtocolService(ProtocolRepository protocolRepository, ProtocolMapper protocolMapper) {
        this.protocolRepository = protocolRepository;
        this.protocolMapper = protocolMapper;
    }

    /**
     * Save a protocol.
     *
     * @param protocolDTO the entity to save.
     * @return the persisted entity.
     */
    public ProtocolDTO save(ProtocolDTO protocolDTO) {
        log.debug("Request to save Protocol : {}", protocolDTO);
        Protocol protocol = protocolMapper.toEntity(protocolDTO, new CycleAvoidingMappingContext());
        protocol = protocolRepository.save(protocol);
        return protocolMapper.toDto(protocol, new CycleAvoidingMappingContext());
    }

    /**
     * Update a protocol.
     *
     * @param protocolDTO the entity to save.
     * @return the persisted entity.
     */
    public ProtocolDTO update(ProtocolDTO protocolDTO) {
        log.debug("Request to update Protocol : {}", protocolDTO);
        Protocol protocol = protocolMapper.toEntity(protocolDTO, new CycleAvoidingMappingContext());
        protocol = protocolRepository.save(protocol);
        return protocolMapper.toDto(protocol, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a protocol.
     *
     * @param protocolDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ProtocolDTO> partialUpdate(ProtocolDTO protocolDTO) {
        log.debug("Request to partially update Protocol : {}", protocolDTO);

        return protocolRepository
            .findById(protocolDTO.getId())
            .map(existingProtocol -> {
                protocolMapper.partialUpdate(existingProtocol, protocolDTO);

                return existingProtocol;
            })
            .map(protocolRepository::save)
            .map(o -> protocolMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the protocols.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<ProtocolDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Protocols");
        return protocolRepository.findAll(pageable).map(o -> protocolMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get one protocol by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ProtocolDTO> findOne(String id) {
        log.debug("Request to get Protocol : {}", id);
        return protocolRepository.findById(id).map(o -> protocolMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public Optional<ProtocolDTO> findByCode(String code) {
        log.debug("Request to get Protocol by code: {}", code);
        return protocolRepository.findByCode(code).map(o -> protocolMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the protocol by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete Protocol : {}", id);
        protocolRepository.deleteById(id);
    }
}
