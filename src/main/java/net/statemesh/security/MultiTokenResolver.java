package net.statemesh.security;

import jakarta.servlet.http.HttpServletRequest;
import net.statemesh.domain.NodeReservation;
import net.statemesh.repository.NodeReservationRepository;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.statemesh.config.Constants.OPENCOST_AUTH_HEADER;
import static net.statemesh.config.Constants.SM_ID_HEADER;

public final class MultiTokenResolver implements BearerTokenResolver {
    public static final String ACCESS_TOKEN_PARAMETER_NAME = "access_token";
    private static final Pattern authorizationPattern = Pattern.compile("^Bearer (?<token>[a-zA-Z0-9-._~+/]+=*)$", 2);

    private final NodeReservationRepository nodeReservationRepository;
    private boolean allowUriQueryParameter = false;

    public MultiTokenResolver(NodeReservationRepository nodeReservationRepository) {
        this.nodeReservationRepository = nodeReservationRepository;
    }

    public String resolve(final HttpServletRequest request) {
        if (request.getRequestURI().startsWith("/api/lakefs-s3/")) {
            return null;
        }
        String authorizationHeaderToken = this.resolveFromAuthorizationHeader(request);
        String smIdAuthorizationHeaderToken = this.resolveFromSmIdHeader(request);
        String opencostHeaderToken = this.resolveFromOpencostAuthTokenHeader(request);
        String parameterToken = this.isParameterTokenSupportedForRequest(request) ?
            resolveFromRequestParameters(request) : null;

        if (authorizationHeaderToken != null) {
            if (smIdAuthorizationHeaderToken != null || parameterToken != null) {
                BearerTokenError error = BearerTokenErrors.invalidRequest("Found multiple bearer tokens in the request");
                throw new OAuth2AuthenticationException(error);
            } else {
                return authorizationHeaderToken;
            }
        }

        if (opencostHeaderToken != null) {
            return opencostHeaderToken;
        } else {
            if (smIdAuthorizationHeaderToken != null) {
                if (parameterToken != null) {
                    BearerTokenError error = BearerTokenErrors.invalidRequest("Found multiple bearer tokens in the request");
                    throw new OAuth2AuthenticationException(error);
                } else {
                    return nodeReservationRepository
                        .findActiveReservationForShortSmId(smIdAuthorizationHeaderToken)
                        .map(NodeReservation::getSmId)
                        .orElse(
                            nodeReservationRepository.findFulfilledReservationForShortSmId(smIdAuthorizationHeaderToken)
                                .map(NodeReservation::getSmId)
                                .orElse(smIdAuthorizationHeaderToken)
                        );
                }
            } else {
                return parameterToken != null && this.isParameterTokenEnabledForRequest(request) ? parameterToken : null;
            }
        }
    }

    private String resolveFromAuthorizationHeader(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.startsWithIgnoreCase(authorization, "bearer")) {
            return null;
        } else {
            Matcher matcher = authorizationPattern.matcher(authorization);
            if (!matcher.matches()) {
                BearerTokenError error = BearerTokenErrors.invalidToken("Bearer token is malformed");
                throw new OAuth2AuthenticationException(error);
            } else {
                return matcher.group("token");
            }
        }
    }

    private String resolveFromSmIdHeader(HttpServletRequest request) {
        return request.getHeader(SM_ID_HEADER);
    }

    private String resolveFromOpencostAuthTokenHeader(HttpServletRequest request) {
        return request.getHeader(OPENCOST_AUTH_HEADER);
    }

    public MultiTokenResolver allowUriQueryParameter(boolean allowUriQueryParameter) {
        this.allowUriQueryParameter = allowUriQueryParameter;
        return this;
    }

    private static String resolveFromRequestParameters(HttpServletRequest request) {
        String[] values = request.getParameterValues(ACCESS_TOKEN_PARAMETER_NAME);
        if (values != null && values.length != 0) {
            if (values.length == 1) {
                return values[0];
            } else {
                BearerTokenError error = BearerTokenErrors.invalidRequest("Found multiple bearer tokens in the request");
                throw new OAuth2AuthenticationException(error);
            }
        } else {
            return null;
        }
    }

    private boolean isParameterTokenSupportedForRequest(final HttpServletRequest request) {
        return isFormEncodedRequest(request) || isGetRequest(request);
    }

    private static boolean isGetRequest(HttpServletRequest request) {
        return HttpMethod.GET.name().equals(request.getMethod());
    }

    private static boolean isFormEncodedRequest(HttpServletRequest request) {
        return "application/x-www-form-urlencoded".equals(request.getContentType());
    }

    private boolean isParameterTokenEnabledForRequest(HttpServletRequest request) {
        return this.allowUriQueryParameter && isGetRequest(request);
    }
}
