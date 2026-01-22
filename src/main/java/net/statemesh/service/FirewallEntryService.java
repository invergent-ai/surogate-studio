package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import net.statemesh.domain.FirewallEntry;
import net.statemesh.repository.FirewallEntryRepository;
import net.statemesh.service.dto.FirewallEntryDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.FirewallEntryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link FirewallEntry}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FirewallEntryService {
    private final Logger log = LoggerFactory.getLogger(FirewallEntryService.class);

    private final FirewallEntryRepository firewallEntryRepository;
    private final FirewallEntryMapper firewallEntryMapper;

    /**
     * Save a firewallEntry.
     *
     * @param firewallEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public FirewallEntryDTO save(FirewallEntryDTO firewallEntryDTO) {
        log.debug("Request to save FirewallEntry : {}", firewallEntryDTO);
        FirewallEntry firewallEntry = firewallEntryMapper.toEntity(firewallEntryDTO, new CycleAvoidingMappingContext());
        firewallEntry = firewallEntryRepository.save(firewallEntry);
        return firewallEntryMapper.toDto(firewallEntry, new CycleAvoidingMappingContext());
    }

    /**
     * Update a firewallEntry.
     *
     * @param firewallEntryDTO the entity to save.
     * @return the persisted entity.
     */
    public FirewallEntryDTO update(FirewallEntryDTO firewallEntryDTO) {
        log.debug("Request to update FirewallEntry : {}", firewallEntryDTO);
        FirewallEntry firewallEntry = firewallEntryMapper.toEntity(firewallEntryDTO, new CycleAvoidingMappingContext());
        firewallEntry = firewallEntryRepository.save(firewallEntry);
        return firewallEntryMapper.toDto(firewallEntry, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a firewallEntry.
     *
     * @param firewallEntryDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<FirewallEntryDTO> partialUpdate(FirewallEntryDTO firewallEntryDTO) {
        log.debug("Request to partially update FirewallEntry : {}", firewallEntryDTO);

        return firewallEntryRepository
            .findById(firewallEntryDTO.getId())
            .map(existingFirewallEntry -> {
                firewallEntryMapper.partialUpdate(existingFirewallEntry, firewallEntryDTO);

                return existingFirewallEntry;
            })
            .map(firewallEntryRepository::save)
            .map(o -> firewallEntryMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the firewallEntry.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<FirewallEntryDTO> findAll() {
        log.debug("Request to get all FirewallEntries");
        return firewallEntryRepository
            .findAll()
            .stream()
            .map(o -> firewallEntryMapper.toDto(o, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one firewallEntry by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<FirewallEntryDTO> findOne(String id) {
        log.debug("Request to get FirewallEntry : {}", id);
        return firewallEntryRepository.findById(id).map(
            o -> firewallEntryMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the firewallEntry by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete FirewallEntry : {}", id);
        firewallEntryRepository.deleteById(id);
    }
}
