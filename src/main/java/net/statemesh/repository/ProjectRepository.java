package net.statemesh.repository;

import net.statemesh.domain.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the Project entity.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String>, JpaSpecificationExecutor<Project> {
    default Optional<Project> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }
    List<Project> findByNameContainingIgnoreCase(String name);

    default List<Project> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Project> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select project from Project project left join fetch project.user left join fetch project.organization left join fetch project.cluster",
        countQuery = "select count(project) from Project project"
    )
    Page<Project> findAllWithToOneRelationships(Pageable pageable);

    @Query(
        "select project from Project project left join fetch project.user left join fetch project.organization left join fetch project.cluster"
    )
    List<Project> findAllWithToOneRelationships();

    @Query(
        "select project from Project project left join fetch project.user left join fetch project.organization left join fetch project.cluster where project.id =:id"
    )
    Optional<Project> findOneWithToOneRelationships(@Param("id") String id);

    List<Project> findByUser_Login(String userLogin);

    @Query("select count(p) from Project p left join p.user user where user.login =:login")
    int countUserProjects(@Param("login") String login);
}
