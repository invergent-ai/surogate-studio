package net.statemesh.repository;

import net.statemesh.domain.UserXOrganization;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the UserXOrganization entity.
 */
@SuppressWarnings("unused")
@Repository
public interface UserXOrganizationRepository extends JpaRepository<UserXOrganization, String> {}
