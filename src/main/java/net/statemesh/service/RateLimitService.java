package net.statemesh.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import net.statemesh.config.ApplicationProperties;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final ApplicationProperties applicationProperties;
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> smIdCache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String apiKey) {
        return cache.computeIfAbsent(apiKey, this::newBucket);
    }

    public Bucket resolveSmIdBucket(String smId) {
        return smIdCache.computeIfAbsent(smId, this::newSmIdBucket);
    }

    private Bucket newBucket(String apiKey) {
        return Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(applicationProperties.getRateLimiter().getRateLimitTokens())
                    .refillIntervally(applicationProperties.getRateLimiter().getRateLimitTokens(), Duration.ofMinutes(1))
                    .build()
            )
            .build();
    }

    private Bucket newSmIdBucket(String smId) {
        return Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(applicationProperties.getRateLimiter().getSmidRateLimitTokens())
                    .refillIntervally(applicationProperties.getRateLimiter().getSmidRateLimitTokens(), Duration.ofSeconds(10))
                    .build()
            )
            .build();
    }
}
