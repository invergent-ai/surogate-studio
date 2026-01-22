package net.statemesh.repository;

import net.statemesh.domain.Application;
import net.statemesh.domain.enumeration.ApplicationStatus;
import net.statemesh.k8s.util.ResourceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Application entity.
 */
@Repository
public interface ApplicationRepository extends JpaRepository<Application, String>, JpaSpecificationExecutor<Application> {
    default Optional<Application> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }
    List<Application> findByNameContainingIgnoreCase(String name);

    default List<Application> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Application> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select application from Application application left join fetch application.project",
        countQuery = "select count(application) from Application application"
    )
    Page<Application> findAllWithToOneRelationships(Pageable pageable);

    @Query("select application from Application application left join fetch application.project")
    List<Application> findAllWithToOneRelationships();

    @Query("select application from Application application left join fetch application.project where application.id =:id")
    Optional<Application> findOneWithToOneRelationships(@Param("id") String id);

    @Query("select application from Application application left join application.project project" +
        " left join project.user user where user.login =:login")
    List<Application> findUserApplications(@Param("login") String login);

    @Query("select count(application) from Application application left join application.project project" +
        " left join project.user user where user.login =:login")
    int countUserApplications(@Param("login") String login);

    @Modifying
    @Query("update Application application set application.status =:status where application.id =:id")
    void updateApplicationStatus(@Param("id") String id, @Param("status") ApplicationStatus status);
}
