package net.statemesh.k8s.task.secret;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Secret;
import net.statemesh.k8s.task.BaseMutationTask;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStub;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static net.statemesh.k8s.util.K8SConstants.*;

public class TokenTask extends BaseMutationTask<String> {
    private final Logger log = LoggerFactory.getLogger(TokenTask.class);

    public TokenTask(ApiStub apiStub, TaskConfig taskConfig, String namespace) {
        super(apiStub, taskConfig, namespace);
    }

    @Override
    public CompletableFuture<TaskResult<String>> call() {
        log.info("Generating bootstrap token on {}", getApiStub().getApiClient().getBasePath());

        final String tokenId = RandomStringUtils.secure().nextAlphanumeric(BOOTSTRAP_TOKEN_ID_BYTES).toLowerCase();
        final String tokenSecret = RandomStringUtils.secure().nextAlphanumeric(BOOTSTRAP_TOKEN_SECRET_BYTES).toLowerCase();

        V1Secret secret;
        try {
            secret = getApiStub().getCoreV1Api().createNamespacedSecret(
                SYSTEM_NAMESPACE,
                new V1Secret()
                    .metadata(
                        new V1ObjectMeta()
                            .namespace(SYSTEM_NAMESPACE)
                            .name(Strings.concat(BOOTSTRAP_TOKEN_SECRET_PREFIX, tokenId))
                    )
                    .type(BOOTSTRAP_TOKEN_SECRET_TYPE)
                    .data(
                        Map.of(
                            BOOTSTRAP_TOKEN_ID_KEY, tokenId.getBytes(),
                            BOOTSTRAP_TOKEN_SECRET_KEY, tokenSecret.getBytes(),
                            Strings.concat(BOOTSTRAP_TOKEN_USAGE_PREFIX, BOOTSTRAP_TOKEN_USAGE_SIGNING), "true".getBytes(),
                            Strings.concat(BOOTSTRAP_TOKEN_USAGE_PREFIX, BOOTSTRAP_TOKEN_USAGE_AUTH), "true".getBytes(),
                            BOOTSTRAP_TOKEN_EXTRA_GROUPS_KEY, String.join(",", BOOTSTRAP_TOKEN_GROUPS).getBytes(),
                            BOOTSTRAP_TOKEN_EXPIRATION_KEY, tokenExpiration()
                        )
                    )
            ).execute();
        } catch(ApiException e) {
            log.error("Token could not be generated with message {}", e.getMessage());
            return CompletableFuture.completedFuture(
                TaskResult.<String>builder()
                    .success(Boolean.FALSE)
                    .build()
            );
        }

        return CompletableFuture.completedFuture(
            TaskResult.<String>builder()
                .success(Boolean.TRUE)
                .creationStatus(TaskResult.CreationStatus.CREATED)
                .value(formatToken(secret, tokenId, tokenSecret))
                .build()
        );
    }

    private byte[] tokenExpiration() {
        return ZonedDateTime
            .now(ZoneOffset.UTC)
            .plusSeconds(getTaskConfig().tokenTTL())
            .format(DateTimeFormatter.ofPattern(RFC3339))
            .getBytes();
    }

    private String formatToken(V1Secret secret, String tokenId, String tokenSecret) {
        if (secret == null) {
            return null;
        }

        return String.join("",
            new String[] {
                BOOTSTRAP_TOKEN_PREFIX,
                hashCA(),
                "::",
                tokenId, ".", tokenSecret
        });
    }

    private String hashCA() {
        try {
            getApiStub().getApiClient().getSslCaCert().reset();
            byte[] caBytes = IOUtils.toByteArray(getApiStub().getApiClient().getSslCaCert());
            byte[] sha256 = MessageDigest.getInstance("SHA-256").digest(caBytes);
            return Hex.encodeHexString(sha256);
        } catch (IOException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
