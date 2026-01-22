package net.statemesh.repository;

import net.statemesh.domain.SystemConfiguration;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the SystemConfiguration entity.
 */
@SuppressWarnings("unused")
@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, String> {}
