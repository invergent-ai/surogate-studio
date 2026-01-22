package net.statemesh.repository;

import net.statemesh.domain.Container;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the Container entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ContainerRepository extends JpaRepository<Container, String>, JpaSpecificationExecutor<Container> {
    List<Container> findAllByApplication_Id(String applicationId);
}
