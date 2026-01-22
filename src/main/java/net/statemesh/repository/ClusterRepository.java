package net.statemesh.repository;

import java.util.List;
import java.util.Optional;
import net.statemesh.domain.Cluster;
import net.statemesh.service.dto.ClusterDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Cluster entity.
 */
@Repository
public interface ClusterRepository extends JpaRepository<Cluster, String> {
    default Optional<Cluster> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Cluster> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Cluster> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select cluster from Cluster cluster left join fetch cluster.zone",
        countQuery = "select count(cluster) from Cluster cluster"
    )
    Page<Cluster> findAllWithToOneRelationships(Pageable pageable);

    @Query("select cluster from Cluster cluster left join fetch cluster.zone")
    List<Cluster> findAllWithToOneRelationships();

    @Query("select cluster from Cluster cluster left join fetch cluster.zone where cluster.id =:id")
    Optional<Cluster> findOneWithToOneRelationships(@Param("id") String id);

    Optional<Cluster> findFirstByCid(String cid);

    List<Cluster> findAllByZoneId(String zoneId);
}
