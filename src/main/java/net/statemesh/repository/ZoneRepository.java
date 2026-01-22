package net.statemesh.repository;

import net.statemesh.domain.Zone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Zone entity.
 */
@Repository
public interface ZoneRepository extends JpaRepository<Zone, String> {
    default Optional<Zone> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<Zone> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Zone> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(value = "select zone from Zone zone left join fetch zone.organization", countQuery = "select count(zone) from Zone zone")
    Page<Zone> findAllWithToOneRelationships(Pageable pageable);

    @Query("select zone from Zone zone left join fetch zone.organization")
    List<Zone> findAllWithToOneRelationships();

    @Query("select zone from Zone zone left join fetch zone.organization where zone.id =:id")
    Optional<Zone> findOneWithToOneRelationships(@Param("id") String id);

    Optional<Zone> findByZoneIdAndOrganization_Id(String zoneId, String organizationId);

    List<Zone> findAllByOrganizationId(String organizationId);
}
