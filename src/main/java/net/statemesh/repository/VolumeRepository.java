package net.statemesh.repository;

import net.statemesh.domain.Volume;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;

/**
 * Spring Data JPA repository for the Volume entity.
 */
@Repository
public interface VolumeRepository extends JpaRepository<Volume, String>, JpaSpecificationExecutor<Volume> {
    @Query("SELECT DISTINCT v FROM Volume v " +
        "WHERE v.project.id IN :projectIds")
    Set<Volume> findAllByProjectIds(@Param("projectIds") Set<String> projectIds);
}
