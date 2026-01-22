package net.statemesh.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.statemesh.domain.UserXOrganization;
import net.statemesh.repository.UserXOrganizationRepository;
import net.statemesh.service.dto.UserXOrganizationDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.UserXOrganizationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link net.statemesh.domain.UserXOrganization}.
 */
@Service
@Transactional
public class UserXOrganizationService {
    private final Logger log = LoggerFactory.getLogger(UserXOrganizationService.class);

    private final UserXOrganizationRepository userXOrganizationRepository;
    private final UserXOrganizationMapper userXOrganizationMapper;

    public UserXOrganizationService(
        UserXOrganizationRepository userXOrganizationRepository,
        UserXOrganizationMapper userXOrganizationMapper
    ) {
        this.userXOrganizationRepository = userXOrganizationRepository;
        this.userXOrganizationMapper = userXOrganizationMapper;
    }

    /**
     * Save a userXOrganization.
     *
     * @param userXOrganizationDTO the entity to save.
     * @return the persisted entity.
     */
    public UserXOrganizationDTO save(UserXOrganizationDTO userXOrganizationDTO) {
        log.debug("Request to save UserXOrganization : {}", userXOrganizationDTO);
        UserXOrganization userXOrganization = userXOrganizationMapper.toEntity(userXOrganizationDTO, new CycleAvoidingMappingContext());
        userXOrganization = userXOrganizationRepository.save(userXOrganization);
        return userXOrganizationMapper.toDto(userXOrganization, new CycleAvoidingMappingContext());
    }

    /**
     * Update a userXOrganization.
     *
     * @param userXOrganizationDTO the entity to save.
     * @return the persisted entity.
     */
    public UserXOrganizationDTO update(UserXOrganizationDTO userXOrganizationDTO) {
        log.debug("Request to update UserXOrganization : {}", userXOrganizationDTO);
        UserXOrganization userXOrganization = userXOrganizationMapper.toEntity(userXOrganizationDTO, new CycleAvoidingMappingContext());
        userXOrganization = userXOrganizationRepository.save(userXOrganization);
        return userXOrganizationMapper.toDto(userXOrganization, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a userXOrganization.
     *
     * @param userXOrganizationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<UserXOrganizationDTO> partialUpdate(UserXOrganizationDTO userXOrganizationDTO) {
        log.debug("Request to partially update UserXOrganization : {}", userXOrganizationDTO);

        return userXOrganizationRepository
            .findById(userXOrganizationDTO.getId())
            .map(existingUserXOrganization -> {
                userXOrganizationMapper.partialUpdate(existingUserXOrganization, userXOrganizationDTO);

                return existingUserXOrganization;
            })
            .map(userXOrganizationRepository::save)
            .map(o -> userXOrganizationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the userXOrganizations.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<UserXOrganizationDTO> findAll() {
        log.debug("Request to get all UserXOrganizations");
        return userXOrganizationRepository
            .findAll()
            .stream()
            .map(o -> userXOrganizationMapper.toDto(o, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one userXOrganization by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<UserXOrganizationDTO> findOne(String id) {
        log.debug("Request to get UserXOrganization : {}", id);
        return userXOrganizationRepository.findById(id)
            .map(o -> userXOrganizationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the userXOrganization by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete UserXOrganization : {}", id);
        userXOrganizationRepository.deleteById(id);
    }
}
