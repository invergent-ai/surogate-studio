package net.statemesh.repository;

import net.statemesh.domain.RayJob;
import net.statemesh.domain.enumeration.RayJobProvisioningStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RayJobRepository extends JpaRepository<RayJob, String>, JpaSpecificationExecutor<RayJob> {
    default Optional<RayJob> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }

    default List<RayJob> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<RayJob> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select rayJob from RayJob rayJob left join fetch rayJob.project",
        countQuery = "select count(rayJob) from RayJob rayJob"
    )
    Page<RayJob> findAllWithToOneRelationships(Pageable pageable);

    @Query("select rayJob from RayJob rayJob left join fetch rayJob.project")
    List<RayJob> findAllWithToOneRelationships();

    @Query("select rayJob from RayJob rayJob left join fetch rayJob.project where rayJob.id =:id")
    Optional<RayJob> findOneWithToOneRelationships(@Param("id") String id);

    @Modifying
    @Query("update RayJob rayJob set rayJob.provisioningStatus =:status where rayJob.id =:id")
    void updateRayJobProvisioningStatus(@Param("id") String id, @Param("status") RayJobProvisioningStatus status);

    @Modifying
    @Query("update RayJob rayJob set rayJob.completedStatus =:status where rayJob.id =:id")
    void updateRayJobCompletedStatus(@Param("id") String id, @Param("status") String status);

    @Modifying
    @Query("update RayJob rayJob set rayJob.startTime =:startTime where rayJob.id =:id")
    void updateStartTime(@Param("id") String id, @Param("startTime") Instant startTime);

    @Modifying
    @Query("update RayJob rayJob set rayJob.endTime =:endTime where rayJob.id =:id")
    void updateEndTime(@Param("id") String id, @Param("endTime") Instant endTime);

    @Modifying
    @Query("update RayJob rayJob set rayJob.submissionId =:submissionId where rayJob.id =:id")
    void updateSubmissionId(@Param("id") String id, @Param("submissionId") String submissionId);

    @Modifying
    @Query("update RayJob rayJob set rayJob.podName =:podName, rayJob.container = :container where rayJob.id =:id")
    void updatePodAndContainer(@Param("id") String id, @Param("podName") String podName, @Param("container") String container);
}
