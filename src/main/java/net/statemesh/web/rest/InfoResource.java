package net.statemesh.web.rest;

import lombok.RequiredArgsConstructor;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.service.dto.StringWrapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/info")
@RequiredArgsConstructor
public class InfoResource {
    private final ApplicationProperties applicationProperties;

    @GetMapping("/url")
    @Cacheable("url")
    @Transactional(readOnly = true)
    public StringWrapper getUrl() {
        return StringWrapper.of(applicationProperties.getClientUrl());
    }

    /**
     * Cache eviction scheduled task that invalidates the nodeStats cache every hour
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    @CacheEvict(value = { "url" }, allEntries = true)
    public void evictCaches() {
        // This method will clear the cache every hour
    }
}
