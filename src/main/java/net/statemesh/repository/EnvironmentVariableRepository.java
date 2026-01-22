package net.statemesh.repository;

import net.statemesh.domain.EnvironmentVariable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the EnvironmentVariable entity.
 */
@SuppressWarnings("unused")
@Repository
public interface EnvironmentVariableRepository extends JpaRepository<EnvironmentVariable, String> {}
