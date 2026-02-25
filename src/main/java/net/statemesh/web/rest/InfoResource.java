package net.statemesh.web.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Info", description = "Application info and configuration")
public class InfoResource {
    private final ApplicationProperties applicationProperties;

    @Operation(summary = "Get client URL")
    @ApiResponse(responseCode = "200", description = "Client URL returned")
    @GetMapping("/url")
    @Cacheable("url")
    @Transactional(readOnly = true)
    public StringWrapper getUrl() {
        return StringWrapper.of(applicationProperties.getClientUrl());
    }

    @Scheduled(fixedRate = 3600000)
    @CacheEvict(value = { "url" }, allEntries = true)
    public void evictCaches() {
    }
}
