package net.statemesh.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.statemesh.security.JwtService;
import net.statemesh.service.UserService;
import net.statemesh.web.rest.vm.LoginVM;
import org.apache.commons.lang3.StringUtils;
import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

import static net.statemesh.config.Constants.CLI_SESSION_PASSWORD;

/**
 * Controller to authenticate users.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthenticationResource {
    private final Logger log = LoggerFactory.getLogger(AuthenticationResource.class);

    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/authenticate")
    public ResponseEntity<JWTToken> authorize(@Valid @RequestBody LoginVM loginVM) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
            loginVM.getUsername(),
            loginVM.getPassword()
        );

        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtService.createToken(authentication, loginVM.isRememberMe());
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setBearerAuth(jwt);

        if (!StringUtils.isEmpty(loginVM.getCliSession()) && !StringUtils.isEmpty(loginVM.getCliHostname())) {
            BasicTextEncryptor textDecrypter = new BasicTextEncryptor();
            textDecrypter.setPasswordCharArray(CLI_SESSION_PASSWORD.toCharArray());
            var secret = textDecrypter.decrypt(new String(Base64.getDecoder().decode(loginVM.getCliSession())));
            if (!loginVM.getCliHostname().equals(Base64.getEncoder().encodeToString(secret.getBytes()))) {
                throw new RuntimeException("Bad CLI session");
            }

            userService.setCliSession(loginVM.getUsername(), loginVM.getCliSession(), jwt);
        }

        return new ResponseEntity<>(new JWTToken(jwt), httpHeaders, HttpStatus.OK);
    }

    /**
     * {@code GET /authenticate} : check if the user is authenticated, and return its login.
     *
     * @param request the HTTP request.
     * @return the login if the user is authenticated.
     */
    @GetMapping("/authenticate")
    public String isAuthenticated(HttpServletRequest request) {
        log.debug("REST request to check if the current user is authenticated");
        return request.getRemoteUser();
    }

    /**
     * Object to return as body in JWT Authentication.
     */
    @Setter
    @AllArgsConstructor
    public static class JWTToken {
        private String idToken;

        @JsonProperty("id_token")
        String getIdToken() {
            return idToken;
        }
    }
}
