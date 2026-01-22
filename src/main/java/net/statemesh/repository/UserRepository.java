package net.statemesh.repository;

import net.statemesh.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {
    String USERS_BY_LOGIN_CACHE = "usersByLogin";

    Optional<User> findOneByActivationKey(String activationKey);
    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);
    Optional<User> findOneByResetKey(String resetKey);
    Optional<User> findOneByLoginIgnoreCase(String login);
    List<User> findByFullNameContainingIgnoreCase(String name);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByLoginIgnoreCase(String login);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    List<User> findAllByLockedOperatorIsTrue();

    List<User> findAllByLockedUserTimeIsNotNull();

    @Modifying
    @Query("UPDATE User u SET u.lockedUserTime = :time WHERE u.login = :login")
    void lockUserForNonPayingApps(@Param("login") String login, @Param("time") Instant time);

    @Modifying
    @Query("UPDATE User u SET u.cliSession = :cliSession WHERE u.login = :login")
    void setCliSession(@Param("login") String login, @Param("cliSession") String cliSession);

    @Modifying
    @Query("UPDATE User u SET u.cliToken = :cliToken WHERE u.login = :login")
    void setCliToken(@Param("login") String login, @Param("cliToken") String cliToken);

    @Modifying
    @Query("UPDATE User u SET u.cliSession = null, u.cliToken = null WHERE u.login = :login")
    void resetCliSession(@Param("login") String login);

    @Query("select user from User user where user.cliSession =:cliSession")
    Optional<User> findByCliSession(@Param("cliSession") String cliSession);

    Optional<User> findByReferralCode(String referralCode);

    long countByReferredByCode(String referralCode);
}
