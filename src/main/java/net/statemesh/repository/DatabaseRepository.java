package net.statemesh.repository;

import net.statemesh.domain.Database;
import net.statemesh.domain.enumeration.DatabaseStatus;
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
 * Spring Data JPA repository for the Database entity.
 */
@Repository
public interface DatabaseRepository extends JpaRepository<Database, String>, JpaSpecificationExecutor<Database> {
    default Optional<Database> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }
    List<Database> findByNameContainingIgnoreCase(String name);

    default List<Database> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Database> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select database from Database database left join fetch database.project",
        countQuery = "select count(database) from Database database"
    )
    Page<Database> findAllWithToOneRelationships(Pageable pageable);

    @Query("select database from Database database left join fetch database.project")
    List<Database> findAllWithToOneRelationships();

    @Query("select database from Database database left join fetch database.project where database.id =:id")
    Optional<Database> findOneWithToOneRelationships(@Param("id") String id);

    @Modifying
    @Query("update Database database set database.status =:status where database.id =:id")
    void updateStatus(@Param("id") String id, @Param("status") DatabaseStatus status);

    @Modifying
    @Query("update Database database set database.stage =:stage where database.id =:id")
    void updateStage(@Param("id") String id, @Param("stage") ResourceStatus.ResourceStatusStage stage);

    @Query("select db from Database db left join db.project project" +
        " left join project.user user where user.login =:login")
    List<Database> findUserDatabases(@Param("login") String login);

    @Query("select count(db) from Database db left join db.project project" +
        " left join project.user user where user.login =:login")
    int countUserDatabases(@Param("login") String login);
}
