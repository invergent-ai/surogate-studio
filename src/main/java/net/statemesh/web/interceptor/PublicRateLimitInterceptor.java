package net.statemesh.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.statemesh.service.RateLimitService;
import net.statemesh.service.exception.RateLimitException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class PublicRateLimitInterceptor implements HandlerInterceptor {
    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(
        @NotNull HttpServletRequest request,
        @NotNull HttpServletResponse response,
        @NotNull Object handler
    ) throws Exception {
        String clientIp = extractClientIp(request);

        if (!rateLimitService
            .resolveSmIdBucket(clientIp)
            .tryConsumeAndReturnRemaining(1)
            .isConsumed()) {
            throw new RateLimitException("Too many requests");
        }

        return true;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (!StringUtils.isEmpty(xForwardedFor)) {
            // The X-Forwarded-For header contains a comma-separated list of IPs
            // The leftmost IP is the original client IP
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
