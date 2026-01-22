package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import net.statemesh.domain.Zone;
import net.statemesh.repository.ZoneRepository;
import net.statemesh.service.dto.NodeDTO;
import net.statemesh.service.dto.ZoneDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.ZoneMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static net.statemesh.config.Constants.STATE_MESH_ORGANIZATION;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Zone}.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ZoneService {

    private final Logger log = LoggerFactory.getLogger(ZoneService.class);

    private final ZoneRepository zoneRepository;
    private final ZoneMapper zoneMapper;
    private final NodeService nodeService;

    /**
     * Save a zone.
     *
     * @param zoneDTO the entity to save.
     * @return the persisted entity.
     */
    public ZoneDTO save(ZoneDTO zoneDTO) {
        log.debug("Request to save Zone : {}", zoneDTO);
        Zone zone = zoneMapper.toEntity(zoneDTO, new CycleAvoidingMappingContext());
        zone = zoneRepository.save(zone);
        return zoneMapper.toDto(zone, new CycleAvoidingMappingContext());
    }

    /**
     * Update a zone.
     *
     * @param zoneDTO the entity to save.
     * @return the persisted entity.
     */
    public ZoneDTO update(ZoneDTO zoneDTO) {
        log.debug("Request to update Zone : {}", zoneDTO);
        Zone zone = zoneMapper.toEntity(zoneDTO, new CycleAvoidingMappingContext());
        zone = zoneRepository.save(zone);
        return zoneMapper.toDto(zone, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a zone.
     *
     * @param zoneDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ZoneDTO> partialUpdate(ZoneDTO zoneDTO) {
        log.debug("Request to partially update Zone : {}", zoneDTO);

        return zoneRepository
            .findById(zoneDTO.getId())
            .map(existingZone -> {
                zoneMapper.partialUpdate(existingZone, zoneDTO);

                return existingZone;
            })
            .map(zoneRepository::save)
            .map(o -> zoneMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the zones.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ZoneDTO> findAll() {
        log.trace("Request to get all Zones");
        return zoneRepository.findAll().stream()
            .map(this::addInfo)
            .map(o -> zoneMapper.toDto(o, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    private Zone addInfo(Zone zone) {
        List<NodeDTO> zoneNodes = zone.getClusters().stream()
            .map(cluster -> nodeService.findByClusterCid(cluster.getCid()))
            .flatMap(List::stream)
            .toList();

        return zone
            .hasHPC(nodeService.nodeWithHPCExists(zoneNodes))
            .hasGPU(nodeService.nodeWithGPUExists(zoneNodes));
    }

    @Transactional(readOnly = true)
    public Optional<ZoneDTO> findByZoneId(String zoneId) {
        return zoneRepository.findByZoneIdAndOrganization_Id(zoneId, STATE_MESH_ORGANIZATION)
            .map(o -> zoneMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the zones with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ZoneDTO> findAllWithEagerRelationships(Pageable pageable) {
        return zoneRepository.findAllWithEagerRelationships(pageable)
            .map(o -> zoneMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get one zone by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ZoneDTO> findOne(String id) {
        log.debug("Request to get Zone : {}", id);
        return zoneRepository.findOneWithEagerRelationships(id)
            .map(o -> zoneMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public List<ZoneDTO> findByOrganizationId(String organizationId) {
        return zoneRepository.findAllByOrganizationId(organizationId).stream()
            .map(o -> zoneMapper.toDto(o, new CycleAvoidingMappingContext()))
            .collect(Collectors.toList());
    }

    /**
     * Delete the zone by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete Zone : {}", id);
        zoneRepository.deleteById(id);
    }
}
