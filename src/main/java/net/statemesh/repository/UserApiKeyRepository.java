package net.statemesh.repository;

import net.statemesh.domain.UserApiKey;
import net.statemesh.domain.enumeration.ApiKeyProvider;
import net.statemesh.domain.enumeration.ApiKeyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserApiKeyRepository extends JpaRepository<UserApiKey, String> {

    List<UserApiKey> findByUserLogin(String login);

    List<UserApiKey> findByUserLoginAndType(String login, ApiKeyType type);

    Optional<UserApiKey> findByUserLoginAndProviderAndType(String login, ApiKeyProvider provider, ApiKeyType type);

    void deleteByUserLoginAndProviderAndType(String login, ApiKeyProvider provider, ApiKeyType type);
}
