package net.statemesh.repository;

import net.statemesh.domain.FirewallEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the FirewallEntry entity.
 */
@SuppressWarnings("unused")
@Repository
public interface FirewallEntryRepository extends JpaRepository<FirewallEntry, String> {}
