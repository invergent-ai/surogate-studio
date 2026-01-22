package net.statemesh.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class SpaWebFilter extends OncePerRequestFilter {
    private final String clientUrl;

    public SpaWebFilter(String clientUrl) {
        this.clientUrl = clientUrl;
    }

    /**
     * Forwards any unmapped paths (except those containing a period) to the client {@code index.html}.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
        throws ServletException, IOException {
        // Request URI includes the contextPath if any, removed it.
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (
            !path.startsWith("/net") &&
            !path.startsWith("/public") &&
            !path.startsWith("/api") &&
            !path.startsWith("/management") &&
            !path.startsWith("/v3/api-docs") &&
            !path.startsWith("/websocket") &&
            !path.startsWith("/actuator") &&
            !path.contains(".") &&
            path.matches("/(.*)")
        ) {
            response.sendRedirect(clientUrl + "/index.html");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
