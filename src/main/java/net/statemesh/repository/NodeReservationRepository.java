package net.statemesh.repository;

import net.statemesh.domain.NodeReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the NodeReservation entity.
 */
@Repository
public interface NodeReservationRepository extends JpaRepository<NodeReservation, String> {
    @Query(
        "select nodeReservation from NodeReservation nodeReservation left join fetch nodeReservation.user user " +
            "where user.login =:login and " +
            "(nodeReservation.deleted is NULL or nodeReservation.deleted = FALSE) and " +
            "(nodeReservation.fulfilled is NULL or nodeReservation.fulfilled = FALSE) and " +
            "(:currentTime <= nodeReservation.expireTime)"
    )
    Optional<NodeReservation> findActiveReservationForUser(@Param("login") String login,
                                                           @Param("currentTime") Instant currentTime);
    @Query(
        "select nodeReservation from NodeReservation nodeReservation left join fetch nodeReservation.user user " +
            "where user.login =:login and " +
            "(nodeReservation.deleted is NULL or nodeReservation.deleted = FALSE)"
    )
    List<NodeReservation> findAllForUser(@Param("login") String login);

    @Query(
        "select nodeReservation from NodeReservation nodeReservation " +
            "where nodeReservation.shortSmId =:shortSmId and " +
            "(nodeReservation.deleted is NULL or nodeReservation.deleted = FALSE)"
    )
    Optional<NodeReservation> findActiveReservationForShortSmId(@Param("shortSmId") String shortSmId);

    @Query(
        "select nodeReservation from NodeReservation nodeReservation " +
            "where nodeReservation.userKey =:userKey and " +
            "(nodeReservation.deleted is NULL or nodeReservation.deleted = FALSE) and " +
            "(nodeReservation.fulfilled is NULL or nodeReservation.fulfilled = FALSE) and " +
            "(:currentTime <= nodeReservation.expireTime)"
    )
    Optional<NodeReservation> findActiveReservationForUserKey(@Param("userKey") String userKey,
                                                              @Param("currentTime") Instant currentTime);

    @Query(
        "select nodeReservation from NodeReservation nodeReservation " +
            "where nodeReservation.userKey =:userKey and " +
            "(nodeReservation.deleted is NULL or nodeReservation.deleted = FALSE)"
    )
    Optional<NodeReservation> findReservationForUserKey(@Param("userKey") String userKey);

    @Query(
        "select nodeReservation from NodeReservation nodeReservation " +
            "where nodeReservation.shortSmId =:shortSmId and " +
            "(nodeReservation.deleted is NULL or nodeReservation.deleted = FALSE) and " +
            "(nodeReservation.fulfilled = TRUE) and " +
            "(nodeReservation.node is NOT NULL)"
    )
    Optional<NodeReservation> findFulfilledReservationForShortSmId(@Param("shortSmId") String shortSmId);
}
