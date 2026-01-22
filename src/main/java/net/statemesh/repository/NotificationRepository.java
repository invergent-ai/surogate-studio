package net.statemesh.repository;

import java.util.List;
import java.util.Optional;
import net.statemesh.domain.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the Notification entity.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    default Optional<Notification> findOneWithEagerRelationships(String id) {
        return this.findOneWithToOneRelationships(id);
    }
    @Query("SELECT n FROM Notification n WHERE n.user.login = ?1 ORDER BY n.createdTime DESC")
    Page<Notification> findByUserLoginOrderByCreatedTimeDesc(String login, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.login = ?1")
    void markAllAsRead(String login);
    default List<Notification> findAllWithEagerRelationships() {
        return this.findAllWithToOneRelationships();
    }

    default Page<Notification> findAllWithEagerRelationships(Pageable pageable) {
        return this.findAllWithToOneRelationships(pageable);
    }

    @Query(
        value = "select notification from Notification notification left join fetch notification.user",
        countQuery = "select count(notification) from Notification notification"
    )
    Page<Notification> findAllWithToOneRelationships(Pageable pageable);

    @Query("select notification from Notification notification left join fetch notification.user")
    List<Notification> findAllWithToOneRelationships();

    @Query("select notification from Notification notification left join fetch notification.user where notification.id =:id")
    Optional<Notification> findOneWithToOneRelationships(@Param("id") String id);

    Page<Notification> findByUserLoginAndReadIsFalseOrderByCreatedTimeDesc(String login, Pageable pageable);
}
