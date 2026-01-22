package net.statemesh.repository;

import net.statemesh.domain.TaskRun;
import net.statemesh.domain.enumeration.TaskRunProvisioningStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface TaskRunRepository extends JpaRepository<TaskRun, String>, JpaSpecificationExecutor<TaskRun> {
    @Modifying
    @Query("update TaskRun taskRun set taskRun.provisioningStatus =:status where taskRun.id =:id")
    void updateTaskRunProvisioningStatus(@Param("id") String id, @Param("status") TaskRunProvisioningStatus status);

    @Modifying
    @Query("update TaskRun taskRun set taskRun.completedStatus =:status where taskRun.id =:id")
    void updateTaskRunCompletedStatus(@Param("id") String id, @Param("status") String status);

    @Modifying
    @Query("update TaskRun taskRun set taskRun.endTime =:endTime where taskRun.id =:id")
    void updateEndTime(@Param("id") String id, @Param("endTime") Instant endTime);

    @Modifying
    @Query("update TaskRun taskRun set taskRun.startTime =:startTime where taskRun.id =:id")
    void updateStartTime(@Param("id") String id, @Param("startTime") Instant startTime);

    @Modifying
    @Query("update TaskRun taskRun set taskRun.podName =:podName, taskRun.container = :container where taskRun.id =:id")
    void updatePodAndContainer(@Param("id") String id, @Param("podName") String podName, @Param("container") String container);

    default Page<TaskRun> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select taskRun from TaskRun taskRun left join fetch taskRun.project",
        countQuery = "select count(taskRun) from TaskRun taskRun"
    )
    Page<TaskRun> findAllWithToOneRelationships(Pageable pageable);

    default Optional<TaskRun> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }

    @Query("select taskRun from TaskRun taskRun left join fetch taskRun.project where taskRun.id =:id")
    Optional<TaskRun> findOneWithToOneRelationships(@Param("id") String id);
}
