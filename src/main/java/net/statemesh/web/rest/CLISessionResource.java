package net.statemesh.web.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.domain.User;
import net.statemesh.service.UserService;
import net.statemesh.service.dto.PostCLIResponseDTO;
import net.statemesh.service.dto.PreCLIDTO;
import net.statemesh.service.dto.PreCLIResponseDTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

import static net.statemesh.config.Constants.CLI_SESSION_PASSWORD;
import static net.statemesh.security.SecurityUtils.*;

/**
 * REST controller for managing {@link net.statemesh.domain.Application}.
 */
@RestController
@RequestMapping("/api/cli")
@RequiredArgsConstructor
@Slf4j
public class CLISessionResource {
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final UserService userService;

    @PostMapping("/pre")
    public ResponseEntity<PreCLIResponseDTO> precli(@RequestBody PreCLIDTO preCLIDTO) throws URISyntaxException {
        log.debug("REST request to initiate CLI session for hostname : {}", preCLIDTO.getHostname());
        if (StringUtils.isEmpty(preCLIDTO.getHostname())) {
            throw new RuntimeException("Hostname cannot be empty");
        }

        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPasswordCharArray(CLI_SESSION_PASSWORD.toCharArray());
        var secret = Base64.getEncoder().encodeToString(textEncryptor.encrypt(preCLIDTO.getHostname()).getBytes());
        return ResponseEntity
            .created(new URI("/api/precli/" + preCLIDTO.getHostname()))
            .body(
                PreCLIResponseDTO.builder()
                    .browserUrl(
                        StringUtils.join("?sess=", secret, "&sessId=",
                            Base64.getEncoder().encodeToString(preCLIDTO.getHostname().getBytes()))
                    )
                    .token(
                        createToken(secret)
                    )
                    .build()
            );
    }

    @GetMapping("/post")
    public ResponseEntity<PostCLIResponseDTO> postcli(@RequestHeader("Authorization") String token) throws URISyntaxException {
        log.debug("REST request to finalize CLI session");
        if (StringUtils.isEmpty(token)) {
            throw new RuntimeException("Authorization cannot be empty");
        }

        Optional<User> user = userService.findByCliSession(
            jwtDecoder.decode(token).getClaims().get(USER_KEY).toString()
        );
        String accessToken = null;
        if (user.isPresent()) {
            accessToken = user.get().getCliToken();
            userService.resetCliSession(user.get().getLogin());
        }

        return ResponseEntity
            .created(new URI("/api/postcli/"))
            .body(
                PostCLIResponseDTO.builder()
                    .accessToken(accessToken)
                    .build()
            );
    }

    private String createToken(String secret) {
        return this.jwtEncoder
            .encode(
                JwtEncoderParameters.from(
                    JwsHeader.with(JWT_ALGORITHM).build(),
                    JwtClaimsSet.builder()
                        .issuedAt(Instant.now())
                        .expiresAt(
                            Instant.now().plus(10, ChronoUnit.MINUTES)
                        )
                        .subject(secret)
                        .claim(AUTHORITIES_KEY, Strings.EMPTY)
                        .claim(USER_KEY, secret)
                        .build()
                )
            )
            .getTokenValue();
    }
}
