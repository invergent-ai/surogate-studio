package net.statemesh.repository;

import java.util.List;
import java.util.Optional;
import net.statemesh.domain.Port;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Port entity.
 */
@Repository
public interface PortRepository extends JpaRepository<Port, String> {
    default Optional<Port> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Port> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Port> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(value = "select port from Port port left join fetch port.container", countQuery = "select count(port) from Port port")
    Page<Port> findAllWithToOneRelationships(Pageable pageable);

    @Query("select port from Port port left join fetch port.container")
    List<Port> findAllWithToOneRelationships();

    @Query("select port from Port port left join fetch port.container where port.id =:id")
    Optional<Port> findOneWithToOneRelationships(@Param("id") String id);
}
