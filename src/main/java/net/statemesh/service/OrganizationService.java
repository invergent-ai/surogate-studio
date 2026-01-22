package net.statemesh.service;

import net.statemesh.config.Constants;
import net.statemesh.domain.Organization;
import net.statemesh.repository.OrganizationRepository;
import net.statemesh.service.dto.OrganizationDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.OrganizationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Organization}.
 */
@Service
@Transactional
public class OrganizationService {
    private final Logger log = LoggerFactory.getLogger(OrganizationService.class);

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;

    public OrganizationService(OrganizationRepository organizationRepository, OrganizationMapper organizationMapper) {
        this.organizationRepository = organizationRepository;
        this.organizationMapper = organizationMapper;
    }

    /**
     * Save a organization.
     *
     * @param organizationDTO the entity to save.
     * @return the persisted entity.
     */
    public OrganizationDTO save(OrganizationDTO organizationDTO) {
        log.debug("Request to save Organization : {}", organizationDTO);
        Organization organization = organizationMapper.toEntity(organizationDTO, new CycleAvoidingMappingContext());
        organization = organizationRepository.save(organization);
        return organizationMapper.toDto(organization, new CycleAvoidingMappingContext());
    }

    /**
     * Update a organization.
     *
     * @param organizationDTO the entity to save.
     * @return the persisted entity.
     */
    public OrganizationDTO update(OrganizationDTO organizationDTO) {
        log.debug("Request to update Organization : {}", organizationDTO);
        Organization organization = organizationMapper.toEntity(organizationDTO, new CycleAvoidingMappingContext());
        organization = organizationRepository.save(organization);
        return organizationMapper.toDto(organization, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a organization.
     *
     * @param organizationDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<OrganizationDTO> partialUpdate(OrganizationDTO organizationDTO) {
        log.debug("Request to partially update Organization : {}", organizationDTO);

        return organizationRepository
            .findById(organizationDTO.getId())
            .map(existingOrganization -> {
                organizationMapper.partialUpdate(existingOrganization, organizationDTO);

                return existingOrganization;
            })
            .map(organizationRepository::save)
            .map(o -> organizationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the organizations.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<OrganizationDTO> findAll() {
        log.debug("Request to get all Organizations");
        return organizationRepository.findAll().stream()
            .map(o -> organizationMapper.toDto(o, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get one organization by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<OrganizationDTO> findOne(String id) {
        log.debug("Request to get Organization : {}", id);
        return organizationRepository.findById(id)
            .map(o -> organizationMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Delete the organization by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete Organization : {}", id);
        organizationRepository.deleteById(id);
    }

    public OrganizationDTO defaultOrganization() {
        return organizationMapper.toDto(
            organizationRepository.findById(Constants.STATE_MESH_ORGANIZATION)
                .orElseThrow(() -> new RuntimeException("StateMesh organization was not present"))
            , new CycleAvoidingMappingContext()
        );
    }
}
