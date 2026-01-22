package net.statemesh.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import net.statemesh.config.Constants;
import net.statemesh.domain.Cluster;
import net.statemesh.repository.ClusterRepository;
import net.statemesh.service.dto.ClusterDTO;
import net.statemesh.service.mapper.ClusterMapper;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Cluster}.
 */
@Service
@Transactional
public class ClusterService {
    private final Logger log = LoggerFactory.getLogger(ClusterService.class);

    private final ClusterRepository clusterRepository;
    private final ClusterMapper clusterMapper;

    public ClusterService(ClusterRepository clusterRepository, ClusterMapper clusterMapper) {
        this.clusterRepository = clusterRepository;
        this.clusterMapper = clusterMapper;
    }

    /**
     * Save a cluster.
     *
     * @param clusterDTO the entity to save.
     * @return the persisted entity.
     */
    public ClusterDTO save(ClusterDTO clusterDTO) {
        log.debug("Request to save Cluster : {}", clusterDTO);
        Cluster cluster = clusterMapper.toEntity(clusterDTO, new CycleAvoidingMappingContext());
        cluster = clusterRepository.save(cluster);
        return clusterMapper.toDto(cluster, new CycleAvoidingMappingContext());
    }

    /**
     * Update a cluster.
     *
     * @param clusterDTO the entity to save.
     * @return the persisted entity.
     */
    public ClusterDTO update(ClusterDTO clusterDTO) {
        log.debug("Request to update Cluster : {}", clusterDTO);
        Cluster cluster = clusterMapper.toEntity(clusterDTO, new CycleAvoidingMappingContext());
        cluster = clusterRepository.save(cluster);
        return clusterMapper.toDto(cluster, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a cluster.
     *
     * @param clusterDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<ClusterDTO> partialUpdate(ClusterDTO clusterDTO) {
        log.debug("Request to partially update Cluster : {}", clusterDTO);

        return clusterRepository
            .findById(clusterDTO.getId())
            .map(existingCluster -> {
                clusterMapper.partialUpdate(existingCluster, clusterDTO);

                return existingCluster;
            })
            .map(clusterRepository::save)
            .map(c -> clusterMapper.toDto(c, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the clusters.
     *
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public List<ClusterDTO> findAll() {
        log.debug("Request to get all Clusters");
        return clusterRepository.findAll().stream()
            .map(c -> clusterMapper.toDto(c, new CycleAvoidingMappingContext()))
            .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * Get all the clusters with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<ClusterDTO> findAllWithEagerRelationships(Pageable pageable) {
        return clusterRepository.findAllWithEagerRelationships(pageable)
            .map(c -> clusterMapper.toDto(c, new CycleAvoidingMappingContext()));
    }

    /**
     * Get one cluster by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<ClusterDTO> findOne(String id) {
        log.debug("Request to get Cluster : {}", id);
        return clusterRepository.findOneWithEagerRelationships(id)
            .map(c -> clusterMapper.toDto(c, new CycleAvoidingMappingContext()));
    }

    public List<ClusterDTO> findByZoneId(String zoneId) {
        log.debug("Request to get Cluster by Zone : {}", zoneId);
        return clusterRepository.findAllByZoneId(zoneId).stream()
            .map(c -> clusterMapper.toDto(c, new CycleAvoidingMappingContext()))
            .collect(Collectors.toList());
    }

    /**
     * Delete the cluster by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete Cluster : {}", id);
        clusterRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ClusterDTO> findFirstByCid(String clusterId) {
        return clusterRepository.findFirstByCid(clusterId)
            .map(c -> clusterMapper.toDto(c, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public ClusterDTO getApplianceCluster() {
        return clusterRepository.findFirstByCid(Constants.APPLIANCE_CLUSTER_CID)
            .map(c -> clusterMapper.toDto(c, new CycleAvoidingMappingContext()))
            .orElseThrow(() -> new RuntimeException("Appliance cluster not found"));
    }
}
