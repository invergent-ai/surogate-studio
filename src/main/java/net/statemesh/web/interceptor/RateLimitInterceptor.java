package net.statemesh.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import net.statemesh.security.AuthoritiesConstants;
import net.statemesh.security.SecurityUtils;
import net.statemesh.service.RateLimitService;
import net.statemesh.service.exception.BadHeadersException;
import net.statemesh.service.exception.RateLimitException;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static net.statemesh.config.Constants.*;


@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             @NotNull HttpServletResponse response,
                             @NotNull Object handler) {
        if (request.getRequestURI().startsWith("/api/lakefs-s3/")) {
            return true;
        }
        final String referer = request.getHeader("Referer");
        if ("https://checkout.stripe.com/".equals(referer)) {
            return true;
        }

        final String userAgent = request.getHeader("User-Agent");
        if ("meshd".equals(userAgent)) { // TODO - This is prone to DDoS - make/use separate bucket [internals]
            return true;
        }
        if (!StringUtils.isEmpty(request.getHeader(OPENCOST_AUTH_HEADER))) { // TODO - This is prone to DDoS - make/use separate bucket [internals]
            return true;
        }
        if (request.getRequestURL().toString().endsWith("/api/files/download")) { // TODO - This is prone to DDoS and insecure - do authenticated downloads
            return true;
        }

        final String apiKey = request.getHeader(RATE_LIMIT_HEADER);
        final String smId = request.getHeader(SM_ID_HEADER);

        if (StringUtils.isEmpty(apiKey) && StringUtils.isEmpty(smId)) {
            throw new BadHeadersException("Missing Header: " + RATE_LIMIT_HEADER + " or " + SM_ID_HEADER);
        }
        if (!StringUtils.isEmpty(apiKey) && !apiKey.endsWith(RATE_LIMIT_SECURE_END)) {
            throw new BadHeadersException("Wrong Header: " + RATE_LIMIT_HEADER);
        }

        // Disable RateLimiting for Admin users
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return true;
        }

        if (!StringUtils.isEmpty(smId)) {
            if (!rateLimitService
                .resolveSmIdBucket(smId)
                .tryConsumeAndReturnRemaining(1)
                .isConsumed()) {
                throw new RateLimitException("Too many requests [smId]");
            }
        } else {
            if (!rateLimitService
                .resolveBucket(apiKey)
                .tryConsumeAndReturnRemaining(1)
                .isConsumed()) {
                throw new RateLimitException("Too many requests");
            }
        }

        return true;
    }
}
