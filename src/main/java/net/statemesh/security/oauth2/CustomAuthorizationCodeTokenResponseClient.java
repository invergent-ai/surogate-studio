package net.statemesh.security.oauth2;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CustomAuthorizationCodeTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {
    private static final String INVALID_TOKEN_RESPONSE_ERROR_CODE = "invalid_token_response";

    private Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> requestEntityConverter = new OAuth2AuthorizationCodeGrantRequestEntityConverter();

    private RestOperations restOperations;

    public CustomAuthorizationCodeTokenResponseClient() {
        RestTemplate restTemplate = new RestTemplate(
            Arrays.asList(new FormHttpMessageConverter(), new OAuth2AccessTokenResponseHttpMessageConverter()));
        restTemplate.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        this.restOperations = restTemplate;
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(
        OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {
        Assert.notNull(authorizationCodeGrantRequest, "authorizationCodeGrantRequest cannot be null");
        RequestEntity<?> request = this.requestEntityConverter.convert(authorizationCodeGrantRequest);
        ResponseEntity<OAuth2AccessTokenResponse> response = getResponse(request);
        // As per spec, in Section 5.1 Successful Access Token Response
        // https://tools.ietf.org/html/rfc6749#section-5.1
        // If AccessTokenResponse.scope is empty, then we assume all requested scopes were
        // granted.
        // However, we use the explicit scopes returned in the response (if any).
        OAuth2AccessTokenResponse tokenResponse = response.getBody();

        Map<String, Object> additionalParameters = new HashMap<>();
        additionalParameters.putAll(tokenResponse.getAdditionalParameters());
        additionalParameters.putAll(authorizationCodeGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getAdditionalParameters());
        tokenResponse = OAuth2AccessTokenResponse.withResponse(tokenResponse)
            .additionalParameters(additionalParameters)
            .build();

        Assert.notNull(tokenResponse,
            "The authorization server responded to this Authorization Code grant request with an empty body; as such, it cannot be materialized into an OAuth2AccessTokenResponse instance. Please check the HTTP response code in your server logs for more details.");
        return tokenResponse;
    }

    private ResponseEntity<OAuth2AccessTokenResponse> getResponse(RequestEntity<?> request) {
        try {
            return this.restOperations.exchange(request, OAuth2AccessTokenResponse.class);
        }
        catch (RestClientException ex) {
            OAuth2Error oauth2Error = new OAuth2Error(INVALID_TOKEN_RESPONSE_ERROR_CODE,
                "An error occurred while attempting to retrieve the OAuth 2.0 Access Token Response: "
                    + ex.getMessage(),
                null);
            throw new OAuth2AuthorizationException(oauth2Error, ex);
        }
    }

    /**
     * Sets the {@link Converter} used for converting the
     * {@link OAuth2AuthorizationCodeGrantRequest} to a {@link RequestEntity}
     * representation of the OAuth 2.0 Access Token Request.
     * @param requestEntityConverter the {@link Converter} used for converting to a
     * {@link RequestEntity} representation of the Access Token Request
     */
    public void setRequestEntityConverter(
        Converter<OAuth2AuthorizationCodeGrantRequest, RequestEntity<?>> requestEntityConverter) {
        Assert.notNull(requestEntityConverter, "requestEntityConverter cannot be null");
        this.requestEntityConverter = requestEntityConverter;
    }

    /**
     * Sets the {@link RestOperations} used when requesting the OAuth 2.0 Access Token
     * Response.
     *
     * <p>
     * <b>NOTE:</b> At a minimum, the supplied {@code restOperations} must be configured
     * with the following:
     * <ol>
     * <li>{@link HttpMessageConverter}'s - {@link FormHttpMessageConverter} and
     * {@link OAuth2AccessTokenResponseHttpMessageConverter}</li>
     * <li>{@link ResponseErrorHandler} - {@link OAuth2ErrorResponseErrorHandler}</li>
     * </ol>
     * @param restOperations the {@link RestOperations} used when requesting the Access
     * Token Response
     */
    public void setRestOperations(RestOperations restOperations) {
        Assert.notNull(restOperations, "restOperations cannot be null");
        this.restOperations = restOperations;
    }
}
