package net.statemesh.repository;

import net.statemesh.domain.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, String> {

    @Query("SELECT p FROM Provider p WHERE p.active = true ORDER BY p.name")
    List<Provider> findActiveProviders();

    List<Provider> findByActiveOrderByName(Boolean active);
}
