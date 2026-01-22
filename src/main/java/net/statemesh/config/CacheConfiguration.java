package net.statemesh.config;

import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.config.cache.PrefixedKeyGenerator;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private GitProperties gitProperties;
    private BuildProperties buildProperties;
    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    public CacheConfiguration(JHipsterProperties jHipsterProperties) {
        JHipsterProperties.Cache.Ehcache ehcache = jHipsterProperties.getCache().getEhcache();

        jcacheConfiguration =
            Eh107Configuration.fromEhcacheCacheConfiguration(
                CacheConfigurationBuilder
                    .newCacheConfigurationBuilder(Object.class, Object.class, ResourcePoolsBuilder.heap(ehcache.getMaxEntries()))
                    .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.getTimeToLiveSeconds())))
                    .build()
            );
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cacheManager) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            createCache(cm, net.statemesh.repository.UserRepository.USERS_BY_LOGIN_CACHE);
            createCache(cm, net.statemesh.domain.User.class.getName());
            createCache(cm, net.statemesh.domain.Authority.class.getName());
            createCache(cm, net.statemesh.domain.User.class.getName() + ".authorities");
            createCache(cm, net.statemesh.domain.User.class.getName() + ".projects");
            createCache(cm, net.statemesh.domain.User.class.getName() + ".nodes");
            createCache(cm, net.statemesh.domain.User.class.getName() + ".accessLists");
            createCache(cm, net.statemesh.domain.User.class.getName() + ".notifications");
            createCache(cm, net.statemesh.domain.User.class.getName() + ".organizations");
            createCache(cm, net.statemesh.domain.Notification.class.getName());
            createCache(cm, net.statemesh.domain.Organization.class.getName());
            createCache(cm, net.statemesh.domain.Organization.class.getName() + ".projects");
            createCache(cm, net.statemesh.domain.Organization.class.getName() + ".zones");
            createCache(cm, net.statemesh.domain.Organization.class.getName() + ".users");
            createCache(cm, net.statemesh.domain.Project.class.getName());
            createCache(cm, net.statemesh.domain.Project.class.getName() + ".applications");
            createCache(cm, net.statemesh.domain.Project.class.getName() + ".accessLists");
            createCache(cm, net.statemesh.domain.Application.class.getName());
            createCache(cm, net.statemesh.domain.Application.class.getName() + ".annotations");
            createCache(cm, net.statemesh.domain.Application.class.getName() + ".containers");
            createCache(cm, net.statemesh.domain.Container.class.getName());
            createCache(cm, net.statemesh.domain.Container.class.getName() + ".envVars");
            createCache(cm, net.statemesh.domain.Container.class.getName() + ".ports");
            createCache(cm, net.statemesh.domain.Volume.class.getName());
            createCache(cm, net.statemesh.domain.Annotation.class.getName());
            createCache(cm, net.statemesh.domain.EnvironmentVariable.class.getName());
            createCache(cm, net.statemesh.domain.Port.class.getName());
            createCache(cm, net.statemesh.domain.ProjectAccess.class.getName());
            createCache(cm, net.statemesh.domain.Zone.class.getName());
            createCache(cm, net.statemesh.domain.Zone.class.getName() + ".clusters");
            createCache(cm, net.statemesh.domain.Cluster.class.getName());
            createCache(cm, net.statemesh.domain.AppTemplate.class.getName());
            createCache(cm, net.statemesh.domain.Cluster.class.getName() + ".nodes");
            createCache(cm, net.statemesh.domain.Cluster.class.getName() + ".projects");
            createCache(cm, net.statemesh.domain.Node.class.getName());
            createCache(cm, net.statemesh.domain.Node.class.getName() + ".units");
            createCache(cm, net.statemesh.domain.SystemConfiguration.class.getName());
            createCache(cm, net.statemesh.domain.UserXOrganization.class.getName());
            createCache(cm, net.statemesh.domain.Protocol.class.getName());
            createCache(cm, "publicRepos");
            createCache(cm, "imageTags");
            createCache(cm, "imageConfigs");
            createCache(cm, "capacity");
            createCache(cm, "topNodes");
            createCache(cm, "url");
        };
    }

    private void createCache(javax.cache.CacheManager cm, String cacheName) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, jcacheConfiguration);
        }
    }

    @Autowired(required = false)
    public void setGitProperties(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Autowired(required = false)
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return new PrefixedKeyGenerator(this.gitProperties, this.buildProperties);
    }
}
