package net.statemesh.repository;

import net.statemesh.domain.NodeHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the NodeHistory entity.
 */
@Repository
public interface NodeHistoryRepository extends JpaRepository<NodeHistory, String> {
    List<NodeHistory> findAllByNode_Id(String nodeId);
}
