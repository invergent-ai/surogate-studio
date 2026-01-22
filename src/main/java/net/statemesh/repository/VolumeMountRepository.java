package net.statemesh.repository;

import net.statemesh.domain.VolumeMount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for the VolumeMount entity.
 */
@SuppressWarnings("unused")
@Repository
public interface VolumeMountRepository extends JpaRepository<VolumeMount, String>, JpaSpecificationExecutor<VolumeMount> {

    @Query(
        "select mount from VolumeMount mount left join mount.volume " +
            "where mount.id !=:volumeMountId and mount.volume.id =:volumeId"
    )
    List<VolumeMount> otherVolumeMounts(@Param("volumeId") String volumeId, @Param("volumeMountId") String volumeMountId);
}
