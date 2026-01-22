package net.statemesh.service.k8s;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.service.RayJobService;
import net.statemesh.service.dto.ChatMetaDTO;
import net.statemesh.service.dto.LineDTO;
import net.statemesh.service.dto.VLLMMessage;
import net.statemesh.service.dto.VLLMResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.TextMessage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static net.statemesh.k8s.flow.CreateRayJobFlow.VLLM_CONTROLLER_PORT;
import static net.statemesh.k8s.util.NamingUtils.serviceName;

@Service
@Slf4j
@RequiredArgsConstructor
public class TestChatService {
    private static final String START_ENDPOINT = "/start";
    private static final String STOP_ENDPOINT = "/stop";
    private static final String CHAT_ENDPOINT = "/v1/chat/completions";

//    Qwen3 recommendations
    private static final Double TEMPERATURE = 0.7;
    private static final Double TOP_P = 0.8;
    private static final Integer TOP_K = 20;
    private static final Double MIN_P = 0d;
    private static final Double PRESENCE_PENALTY = 1d;
//    ----------------

    private static final Integer MAX_TOKENS = 2048;
    private static final String LORA_ADAPTER = "test-lora"; // Generic adapter name used when starting vLLM
    private static final VLLMMessage.Message systemMessage =
        VLLMMessage.Message.builder().role("system").content("You are a helpful assistant.").build();
    private static final Boolean STREAM = Boolean.TRUE;

    private final RayJobService rayJobService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationProperties applicationProperties;

    private final Map<String, String> urls = new ConcurrentHashMap<>();

    public void startChat(String jobId) {
        final String url = chatUrl(jobId);
        if (url == null) {
            throw new RuntimeException("Chat url could not be found");
        }
        this.urls.put(jobId, url);
        log.debug("Starting chat for {}", jobId);
        ResponseEntity<ChatMetaDTO> response = post(
            StringUtils.join(url, START_ENDPOINT), null, ChatMetaDTO.class
        );
        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from vLLM controller server");
        }
        log.debug("Chat started for {} with message: {}", jobId, response.getBody());
    }

    public void stopChat(String jobId) {
        if (!urls.containsKey(jobId)) {
            throw new RuntimeException("Chat for job " + jobId + " was not started");
        }
        final String url = urls.remove(jobId);
        log.debug("Stopping chat for {}", jobId);
        ResponseEntity<ChatMetaDTO> response = post(
            StringUtils.join(url, STOP_ENDPOINT), null, ChatMetaDTO.class
        );
        if (response.getBody() == null) {
            throw new RuntimeException("Empty response from vLLM controller server");
        }
        log.debug("Chat stopped for {} with message: {}", jobId, response.getBody());
    }

    public void sendMessage(LineDTO messageLine) {
        if (!urls.containsKey(messageLine.getJobId())) {
            throw new RuntimeException("Chat for job " + messageLine.getJobId() + " was not started");
        }

        try {
            final String url = StringUtils.join(
                urls.get(messageLine.getJobId()),
                CHAT_ENDPOINT
            );
            final VLLMMessage message = VLLMMessage.builder()
                .model(isLoRA(messageLine.getJobId()) ? LORA_ADAPTER : null)
                .maxTokens(MAX_TOKENS)
                .temperature(TEMPERATURE)
                .topP(TOP_P)
                .topK(TOP_K)
                .minP(MIN_P)
                .presencePenalty(PRESENCE_PENALTY)
                .chatTemplateKwargs(
                    VLLMMessage.ChatTemplateKwArgs.builder()
                        .enableThinking(Boolean.FALSE)
                        .build()
                )
                .stream(STREAM)
                .messages(List.of(
                    systemMessage,
                    VLLMMessage.Message.builder()
                        .role("user")
                        .content(messageLine.getMessage())
                        .build()
                ))
                .build();

            if (STREAM) {
                stream(url, message,
                    chunk -> sendMessage(messageLine.getJobId(), chunk));
            } else {
                ResponseEntity<VLLMResponse> response = post(
                    url,
                    message,
                    VLLMResponse.class
                );

                if (response.getBody() != null && response.getBody().getChoices() != null
                    && !response.getBody().getChoices().isEmpty()) {
                    sendMessage(
                        messageLine.getJobId(),
                        response.getBody().getChoices().getFirst().getMessage().getContent()
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to send chat message to backend for job {}", messageLine.getJobId(), e);
            messagingTemplate.convertAndSend("/topic/message/" + messageLine.getJobId(),
                Map.of("error", e.getMessage()));
        }
    }

    private void sendMessage(String jobId, String content) {
        try {
            messagingTemplate.convertAndSend("/topic/message/" + jobId, new TextMessage(content));
            log.trace("Successfully sent chat output to websocket for {}", jobId);
        } catch (Exception e) {
            log.error("Failed to send chat output to websocket for job {}",jobId, e);
        }
    }

    private <T> ResponseEntity<T> post(String url, VLLMMessage message, Class<T> clazz) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json");
        try {
            HttpEntity<?> entity =
                new HttpEntity<>(message != null ? objectMapper.writeValueAsString(message) : null, headers);
            return restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                clazz
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void stream(String url, VLLMMessage message, Consumer<String> onDelta) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));
        try {
            byte[] json = objectMapper.writeValueAsBytes(message);
            headers.setContentLength(json.length);
            restTemplate.execute(
                url,
                HttpMethod.POST,
                (ClientHttpRequest req) -> {
                    req.getHeaders().putAll(headers);
                    try (OutputStream os = req.getBody()) {
                        os.write(json);
                    }
                },
                response -> {
                    MediaType ct = response.getHeaders().getContentType();
                    if (ct == null || !MediaType.TEXT_EVENT_STREAM.isCompatibleWith(ct)) {
                        throw new IllegalStateException("Expected text/event-stream, got: " + ct);
                    }
                    try (InputStream is = response.getBody();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            line = line.trim();
                            if (!line.startsWith("data:")) {
                                continue;
                            }
                            String payload = line.substring(5).trim(); // after "data:"
                            if ("[DONE]".equals(payload)) {
                                onDelta.accept(payload);
                                break;
                            }

                            try {
                                JsonNode root = objectMapper.readTree(payload);
                                JsonNode choice = root.path("choices").path(0);
                                String delta = choice.path("delta").path("content").asText(null);
                                if (delta == null || delta.isEmpty()) {
                                    // fallbacks for completions-style or full message chunks
                                    delta = choice.path("text").asText("");
                                    if (delta.isEmpty()) {
                                        delta = choice.path("message").path("content").asText("");
                                    }
                                }
                                if (!delta.isEmpty()) {
                                    onDelta.accept(delta);
                                }
                            } catch (Exception ignoreBadLine) {
                                // ignore keepalives or non-JSON lines
                            }
                        }
                    }
                    return null;
                }
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isLoRA(String jobId) {
        if (StringUtils.isEmpty(jobId)) {
            return Boolean.TRUE;
        }
        var rayJob = rayJobService.findOne(jobId).stream().findAny().orElse(null);
        if (rayJob == null) {
            return Boolean.TRUE;
        }

        return rayJob.getEnvVars().stream()
            .filter(envVar -> "MERGE_LORA".equals(envVar.getKey()))
            .filter(envVar -> "true".equals(envVar.getValue()))
            .findAny()
            .map(envVar -> Boolean.FALSE)
            .orElse(Boolean.TRUE);
    }

    private String chatUrl(String jobId) {
        var rayJob = rayJobService.findOne(jobId).stream().findAny().orElse(null);
        if (rayJob == null) {
            return null;
        }

        return Optional.ofNullable(applicationProperties.getK8sAccessMode()).orElse(Boolean.FALSE) ?
            StringUtils.join(
                "http://",
                serviceName(rayJob.getInternalName(), VLLM_CONTROLLER_PORT.toString()),
                ":",
                VLLM_CONTROLLER_PORT.toString()
            ) :
            StringUtils.join("https://", rayJob.getChatHostName());
    }
}
