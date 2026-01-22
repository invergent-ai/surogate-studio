package net.statemesh.repository;

import net.statemesh.domain.Protocol;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for the Protocol entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ProtocolRepository extends JpaRepository<Protocol, String> {
    Optional<Protocol> findByCode(String name);
}

