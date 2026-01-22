package net.statemesh.repository;

import net.statemesh.domain.NodeResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the NodeResource entity.
 */
@Repository
public interface NodeResourceRepository extends JpaRepository<NodeResource, String> {

}
