package net.statemesh.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.domain.Node;
import net.statemesh.domain.User;
import net.statemesh.domain.enumeration.ComputeType;
import net.statemesh.domain.enumeration.NodeStatus;
import net.statemesh.domain.projections.NodeStatusProjection;
import net.statemesh.repository.NodeRepository;
import net.statemesh.service.dto.NodeDTO;
import net.statemesh.service.dto.UserDTO;
import net.statemesh.service.dto.pub.PublicNodeDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.NodeMapper;
import net.statemesh.service.mapper.pub.PublicNodeMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service Implementation for managing {@link net.statemesh.domain.Node}.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class NodeService {
    private final NodeRepository nodeRepository;
    private final NodeMapper nodeMapper;
    private final PublicNodeMapper publicNodeMapper;

    /**
     * Save a node.
     *
     * @param nodeDTO the entity to save.
     * @return the persisted entity.
     */
    public NodeDTO save(NodeDTO nodeDTO) {
        log.debug("Request to save Node : {}", nodeDTO);
        Node node = nodeMapper.toEntity(nodeDTO, new CycleAvoidingMappingContext());
        node = nodeRepository.save(node);
        return nodeMapper.toDto(node, new CycleAvoidingMappingContext());
    }

    /**
     * Update a node.
     *
     * @param nodeDTO the entity to save.
     * @return the persisted entity.
     */
    public NodeDTO update(NodeDTO nodeDTO) {
        log.debug("Request to update Node : {}", nodeDTO);
        Node node = nodeMapper.toEntity(nodeDTO, new CycleAvoidingMappingContext());
        node = nodeRepository.save(node);
        return nodeMapper.toDto(node, new CycleAvoidingMappingContext());
    }

    /**
     * Partially update a node.
     *
     * @param nodeDTO the entity to update partially.
     * @return the persisted entity.
     */
    public Optional<NodeDTO> partialUpdate(NodeDTO nodeDTO) {
        return nodeRepository
            .findById(nodeDTO.getId())
            .map(existingNode -> {
                // Ensure proper number handling
                if (nodeDTO.getEstimatedNodeCosts() != null) {
                    existingNode.setEstimatedNodeCosts(nodeDTO.getEstimatedNodeCosts());
                }
                nodeMapper.partialUpdate(existingNode, nodeDTO);
                return existingNode;
            })
            .map(nodeRepository::save)
            .map(o -> nodeMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    /**
     * Get all the nodes.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<NodeDTO> findAll(Pageable pageable) {
        log.debug("Request to get all Nodes");
        return nodeRepository.findAll(pageable).map(o -> nodeMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> findAllForUser(User user) {
        log.debug("Request to get all Nodes for user {}", user.getLogin());
        return nodeRepository.findAllByUser_Id(user.getId()).stream()
            .map(node -> nodeMapper.toDto(node, new CycleAvoidingMappingContext()))
            .toList();
    }

    /**
     * Get all the nodes with eager load of many-to-many relationships.
     *
     * @return the list of entities.
     */
    public Page<NodeDTO> findAllWithEagerRelationships(Pageable pageable) {
        return nodeRepository.findAllWithEagerRelationships(pageable).map(o -> nodeMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    public Optional<NodeDTO> findByInternalName(String internalName) {
        return nodeRepository.findByInternalName(internalName)
            .map(node -> nodeMapper.toDto(node, new CycleAvoidingMappingContext()));
    }

    public long countForUser(UserDTO user) {
        return nodeRepository.countAllByUser_Id(user.getId());
    }

    /**
     * Get one node by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<NodeDTO> findOne(String id) {
        log.trace("Request to get Node : {}", id);
        return nodeRepository.findOneWithEagerRelationships(id).map(o -> nodeMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public Optional<NodeStatusProjection> findOneAndProjectStatus(String id) {
        return nodeRepository.findOneById(id);
    }

    @Transactional(readOnly = true)
    public Optional<NodeDTO> findByInternalNameAndClusterCid(String internalName, String clusterId) {
        return nodeRepository.findByInternalNameAndClusterCid(internalName, clusterId)
            .map(o -> nodeMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> findByClusterCid(String clusterId) {
        return nodeRepository.findByClusterCid(clusterId).stream()
            .map(o -> nodeMapper.toDto(o, new CycleAvoidingMappingContext())).toList();
    }

    @Transactional(readOnly = true)
    public List<NodeDTO> findByClusterCidAndUserId(String clusterId, String userId) {
        return nodeRepository.findByClusterCidAndUserId(clusterId, userId).stream()
            .map(o -> nodeMapper.toDto(o, new CycleAvoidingMappingContext())).toList();
    }

    @Transactional(readOnly = true)
    public List<PublicNodeDTO> findAllByDeletedFalseAndStatusReady() {
        return nodeRepository.findAllByDeletedIsNullAndStatus(NodeStatus.READY).stream()
            .map(o -> publicNodeMapper.toDto(o, new CycleAvoidingMappingContext())).toList();
    }

    @Transactional
    public void deleteAll(List<String> ids) {
        log.debug("Request to delete a list of Nodes : {}", ids);
        nodeRepository.findAllById(ids).forEach(Node::delete);
    }

    @Transactional
    public void updateStatus(String id, NodeStatus status) {
        nodeRepository.updateNodeStatus(id, status);
    }

    @Transactional
    public void updateStartTime(String id, Instant startTime) {
        nodeRepository.updateStartTime(id, startTime);
    }

    @Transactional
    public void updateDatacenterName(String id, String datacenterName) {
        nodeRepository.updateDatacenterName(id, datacenterName);
    }

    @Transactional
    public void updateRayCluster(String id, String rayCluster) {
        nodeRepository.updateRayCluster(id, rayCluster);
    }

    public boolean nodeWithGPUExists(List<NodeDTO> nodes) {
        return nodes.stream().anyMatch(node -> ComputeType.GPU.equals(node.getComputeType()));
    }

    public boolean nodeWithHPCExists(List<NodeDTO> nodes) {
        return nodes.stream().anyMatch(node -> !StringUtils.isEmpty(node.getDatacenterName()));
    }

    public boolean nodeWithDatacenterExists(List<NodeDTO> nodes, String datacenterName) {
        return nodes.stream().anyMatch(node -> datacenterName.equalsIgnoreCase(node.getDatacenterName()));
    }
}
