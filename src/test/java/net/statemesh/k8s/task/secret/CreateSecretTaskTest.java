package net.statemesh.k8s.task.secret;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Secret;
import net.statemesh.k8s.task.TaskConfig;
import net.statemesh.k8s.task.TaskResult;
import net.statemesh.k8s.util.ApiStubTestFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CreateSecretTaskTest {

    private static final String NAMESPACE = "test-ns";
    private static final String SECRET_NAME = "my-secret";

    private TaskConfig defaultConfig() {
        // poll interval 0s, timeout 1s -> single fast poll cycle
        return new TaskConfig(1, 2, 2, 1.0, 1.0, false, 60);
    }

    @Test
    @DisplayName("Creates secret when it does not exist and reports CREATED")
    void create_whenNotExists() throws ExecutionException, InterruptedException, ApiException {
        CoreV1Api coreV1Api = mock(CoreV1Api.class, RETURNS_DEEP_STUBS);
        var apiStub = ApiStubTestFactory.builder().coreV1Api(coreV1Api).build();

        // Simulate: first read -> 404 (not found), subsequent read -> returns secret with data
        V1Secret readySecret = new V1Secret();
        readySecret.setData(Map.of("k", new byte[]{1}));

        when(coreV1Api.readNamespacedSecret(eq(SECRET_NAME), eq(NAMESPACE)).execute())
            .thenThrow(new ApiException(404, "Not Found"))
            .thenReturn(readySecret);

        // Mock create call returning created secret (data present so that readiness succeeds on next poll)
        when(coreV1Api.createNamespacedSecret(eq(NAMESPACE), any(V1Secret.class)).execute())
            .thenReturn(readySecret);

        CreateSecretTask task = new CreateSecretTask(
            SECRET_NAME,
            "Opaque",
            Map.of("app", "demo"),
            Map.of("k", new byte[]{1}),
            apiStub,
            defaultConfig(),
            NAMESPACE
        );

        CompletableFuture<TaskResult<Void>> future = task.call();
        TaskResult<Void> result = future.get();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreationStatus()).isEqualTo(TaskResult.CreationStatus.CREATED);
        assertThat(result.isWaitTimeout()).isFalse();

        // Verify create invoked once
        verify(coreV1Api, times(1)).createNamespacedSecret(eq(NAMESPACE), any(V1Secret.class));
    }

    @Test
    @DisplayName("Skips creation when secret already exists and reports SKIPPED_EXISTS")
    void skip_whenExists() throws ExecutionException, InterruptedException, ApiException {
        CoreV1Api coreV1Api = mock(CoreV1Api.class, RETURNS_DEEP_STUBS);
        var apiStub = ApiStubTestFactory.builder().coreV1Api(coreV1Api).build();

        V1Secret existing = new V1Secret();
        existing.setData(Map.of("k", new byte[]{1}));

        // read succeeds immediately -> secret exists
        when(coreV1Api.readNamespacedSecret(eq(SECRET_NAME), eq(NAMESPACE)).execute())
            .thenReturn(existing);

        CreateSecretTask task = new CreateSecretTask(
            SECRET_NAME,
            "Opaque",
            Map.of("app", "demo"),
            Map.of("k", new byte[]{1}),
            apiStub,
            defaultConfig(),
            NAMESPACE
        );

        TaskResult<Void> result = task.call().get();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCreationStatus()).isEqualTo(TaskResult.CreationStatus.SKIPPED_EXISTS);

        // Ensure create path never invoked
        verify(coreV1Api, Mockito.never()).createNamespacedSecret(anyString(), any(V1Secret.class));
    }

    @Test
    @DisplayName("Propagates non-404 ApiException as failed future")
    void fails_whenApiErrorNon404() throws InterruptedException {
        CoreV1Api coreV1Api = mock(CoreV1Api.class, RETURNS_DEEP_STUBS);
        var apiStub = ApiStubTestFactory.builder().coreV1Api(coreV1Api).build();

        // Force secretExists to throw non-404 ApiException -> should propagate
        ApiException apiError = new ApiException(500, "Internal Error");
        try {
            when(coreV1Api.readNamespacedSecret(eq(SECRET_NAME), eq(NAMESPACE)).execute())
                .thenThrow(apiError);
        } catch (ApiException e) {
            // Mockito signature requires catching, but we already have apiError prepared
        }

        CreateSecretTask task = new CreateSecretTask(
            SECRET_NAME,
            "Opaque",
            Map.of("app", "demo"),
            Map.of("k", new byte[]{1}),
            apiStub,
            defaultConfig(),
            NAMESPACE
        );

        CompletableFuture<TaskResult<Void>> future = task.call();
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertThat(ex.getCause()).isInstanceOf(ApiException.class);
        assertThat(((ApiException) ex.getCause()).getCode()).isEqualTo(500);
        verify(coreV1Api, times(1)).readNamespacedSecret(eq(SECRET_NAME), eq(NAMESPACE));
        verify(coreV1Api, never()).createNamespacedSecret(anyString(), any(V1Secret.class));
    }
}
