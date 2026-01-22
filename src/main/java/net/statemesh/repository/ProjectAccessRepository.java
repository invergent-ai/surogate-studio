package net.statemesh.repository;

import java.util.List;
import java.util.Optional;
import net.statemesh.domain.ProjectAccess;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the ProjectAccess entity.
 */
@Repository
public interface ProjectAccessRepository extends JpaRepository<ProjectAccess, String> {
    default Optional<ProjectAccess> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<ProjectAccess> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<ProjectAccess> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select projectAccess from ProjectAccess projectAccess left join fetch projectAccess.user left join fetch projectAccess.project",
        countQuery = "select count(projectAccess) from ProjectAccess projectAccess"
    )
    Page<ProjectAccess> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select projectAccess from ProjectAccess projectAccess left join fetch projectAccess.user left join fetch projectAccess.project"
    )
    List<ProjectAccess> findAllWithToOneRelationships();

    @Query(
        "select projectAccess from ProjectAccess projectAccess left join fetch projectAccess.user left join fetch projectAccess.project where projectAccess.id =:id"
    )
    Optional<ProjectAccess> findOneWithToOneRelationships(@Param("id") String id);
}
