package net.statemesh.service;

import net.statemesh.domain.NodeHistory;
import net.statemesh.repository.NodeHistoryRepository;
import net.statemesh.service.dto.NodeDTO;
import net.statemesh.service.dto.NodeHistoryDTO;
import net.statemesh.service.mapper.CycleAvoidingMappingContext;
import net.statemesh.service.mapper.NodeHistoryMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service Implementation for managing {@link NodeHistory}.
 */
@Service
@Transactional
public class NodeHistoryService {
    private final Logger log = LoggerFactory.getLogger(NodeHistoryService.class);

    private final NodeHistoryRepository nodeHistoryRepository;
    private final NodeHistoryMapper nodeHistoryMapper;

    public NodeHistoryService(NodeHistoryRepository nodeHistoryRepository,
                              NodeHistoryMapper nodeHistoryMapper) {
        this.nodeHistoryRepository = nodeHistoryRepository;
        this.nodeHistoryMapper = nodeHistoryMapper;
    }

    /**
     * Save a nodeHistory.
     *
     * @param nodeHistoryDTO the entity to save.
     * @return the persisted entity.
     */
    public NodeHistoryDTO save(NodeHistoryDTO nodeHistoryDTO) {
        log.debug("Request to save NodeHistory : {}", nodeHistoryDTO);
        NodeHistory nodeHistory = nodeHistoryMapper.toEntity(nodeHistoryDTO, new CycleAvoidingMappingContext());
        nodeHistory = nodeHistoryRepository.save(nodeHistory);
        return nodeHistoryMapper.toDto(nodeHistory, new CycleAvoidingMappingContext());
    }

    /**
     * Update a nodeHistory.
     *
     * @param nodeHistoryDTO the entity to save.
     * @return the persisted entity.
     */
    public NodeHistoryDTO update(NodeHistoryDTO nodeHistoryDTO) {
        log.debug("Request to update NodeHistory : {}", nodeHistoryDTO);
        NodeHistory nodeHistory = nodeHistoryMapper.toEntity(nodeHistoryDTO, new CycleAvoidingMappingContext());
        nodeHistory = nodeHistoryRepository.save(nodeHistory);
        return nodeHistoryMapper.toDto(nodeHistory, new CycleAvoidingMappingContext());
    }

    /**
     * Get all the nodeHistories.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<NodeHistoryDTO> findAll(Pageable pageable) {
        log.debug("Request to get all NodeHistories");
        return nodeHistoryRepository.findAll(pageable).map(o -> nodeHistoryMapper.toDto(o, new CycleAvoidingMappingContext()));
    }

    @Transactional(readOnly = true)
    public List<NodeHistoryDTO> findForNode(NodeDTO nodeDTO) {
        return nodeHistoryMapper.toDto(
            nodeHistoryRepository.findAllByNode_Id(nodeDTO.getId()), new CycleAvoidingMappingContext()
        );
    }

    /**
     * Delete the nodeHistory by id.
     *
     * @param id the id of the entity.
     */
    public void delete(String id) {
        log.debug("Request to delete NodeHistory : {}", id);
        nodeHistoryRepository.deleteById(id);
    }
}
