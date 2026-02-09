package net.statemesh.config;

import lombok.RequiredArgsConstructor;
import net.statemesh.security.AuthoritiesConstants;
import net.statemesh.web.filter.SpaWebFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;
import tech.jhipster.config.JHipsterProperties;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@Profile(Constants.PROFILE_APPLIANCE)
@EnableMethodSecurity(securedEnabled = true)
@RequiredArgsConstructor
public class ApplianceSecurityConfiguration {
    private final JHipsterProperties jHipsterProperties;
    private final ApplicationProperties applicationProperties;

    @Bean
    @Profile(Constants.PROFILE_APPLIANCE)
    public SecurityFilterChain applianceFilterChain(HttpSecurity http, MvcRequestMatcher.Builder mvc) throws Exception {
        http
            .cors(withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterAfter(new SpaWebFilter(applicationProperties.getClientUrl()), BasicAuthenticationFilter.class)
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives(jHipsterProperties.getSecurity().getContentSecurityPolicy()))
                    .frameOptions(FrameOptionsConfig::sameOrigin)
                    .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .authorizeHttpRequests(authz ->
                // prettier-ignore
                authz
                    .requestMatchers(mvc.pattern("/index.html")).permitAll()
                    .requestMatchers(mvc.pattern("/favicon.ico")).permitAll()
                    .requestMatchers(mvc.pattern("/manifest.webapp")).permitAll()
                    .requestMatchers(mvc.pattern("/assets/**")).permitAll()
                    .requestMatchers(mvc.pattern("/content/**")).permitAll()
                    .requestMatchers(mvc.pattern("/resources/**")).permitAll()
                    .requestMatchers(mvc.pattern("/@duckdb/**")).permitAll()
                    .requestMatchers(mvc.pattern("/*.js")).permitAll()
                    .requestMatchers(mvc.pattern("/*.css")).permitAll()
                    .requestMatchers(mvc.pattern("/*.map")).permitAll()
                    .requestMatchers(mvc.pattern("/*.wasm")).permitAll()
                    .requestMatchers(mvc.pattern("/*.worker.js")).permitAll()
                    .requestMatchers(mvc.pattern("/*.mjs")).permitAll()
                    .requestMatchers(mvc.pattern("/app/**")).permitAll()
                    .requestMatchers(mvc.pattern("/net/**")).permitAll()
                    .requestMatchers(mvc.pattern("/i18n/**")).permitAll()
                    .requestMatchers(mvc.pattern("/swagger-ui/**")).permitAll()
                    .requestMatchers(mvc.pattern("/public/**")).permitAll()
                    .requestMatchers(mvc.pattern("/actuator/metrics")).permitAll()
                    .requestMatchers(mvc.pattern("/actuator/metrics/**")).permitAll()
                    .requestMatchers(mvc.pattern("/actuator/prometheus")).permitAll()
                    .requestMatchers(mvc.pattern("/actuator/prometheus/**")).permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/authenticate")).permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/authenticate")).permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.POST, "/api/cli/pre")).permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/cli/post")).permitAll()
                    .requestMatchers(mvc.pattern("/api/files/download")).permitAll()
                    .requestMatchers(mvc.pattern("/api/register")).permitAll()
                    .requestMatchers(mvc.pattern("/api/activate")).permitAll()
                    .requestMatchers(mvc.pattern("/api/account/reset-password/init")).permitAll()
                    .requestMatchers(mvc.pattern("/api/account/reset-password/finish")).permitAll()
                    .requestMatchers(mvc.pattern(HttpMethod.GET, "/api/app-template/**")).permitAll()
                    .requestMatchers(mvc.pattern("/api/info/**")).permitAll()
                    .requestMatchers(mvc.pattern("/api/admin/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                    .requestMatchers(mvc.pattern("/api/lakefs-s3/**")).permitAll()
                    .requestMatchers(mvc.pattern("/api/**")).authenticated()
                    .requestMatchers(mvc.pattern("/websocket/**")).authenticated()
                    .requestMatchers(mvc.pattern("/v3/api-docs/**")).hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions ->
                exceptions
                    .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                    .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    MvcRequestMatcher.Builder mvc(HandlerMappingIntrospector introspector) {
        return new MvcRequestMatcher.Builder(introspector);
    }
}
