package net.statemesh.repository;

import net.statemesh.domain.NodeCondition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the NodeCondition entity.
 */
@Repository
public interface NodeConditionRepository extends JpaRepository<NodeCondition, String> {

}
