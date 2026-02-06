package net.statemesh.service.k8s;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.statemesh.config.ApplicationProperties;
import net.statemesh.domain.Application;
import net.statemesh.repository.ApplicationRepository;
import net.statemesh.service.RayJobService;
import net.statemesh.service.dto.*;
import net.statemesh.service.dto.vllm.MessageInfoDTO;
import net.statemesh.service.dto.vllm.UsageDTO;
import net.statemesh.service.dto.vllm.VllmChatRequestDTO;
import net.statemesh.service.dto.vllm.VllmChatResponseDTO;
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
    private static final String TEST_LORA_ADAPTER = "test-lora"; // Generic adapter name used when starting vLLM after training
    private static final VLLMMessage.Message systemMessage =
        VLLMMessage.Message.builder().role("system").content("You are a helpful assistant.").build();
    private static final Boolean STREAM = Boolean.TRUE;

    private final RayJobService rayJobService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RestTemplate restTemplate;

    @Resource(name = "vllmRestTemplate")
    private RestTemplate vllmRestTemplate;

    private final ObjectMapper objectMapper;
    private final ApplicationRepository applicationRepository;
    private final ApplicationProperties applicationProperties;
    private final ObjectMapper yamlMapper = new ObjectMapper(
        YAMLFactory.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .build()
    )
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    private final Map<String, String> urls = new ConcurrentHashMap<>();
    private final Map<String, Boolean> vllmActiveStreams = new ConcurrentHashMap<>();

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
                .model(isLoRA(messageLine.getJobId()) ? TEST_LORA_ADAPTER : null)
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
        try {
            yamlMapper.configure(
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                false
            );
            TrainingConfigDTO trainingConfig =
                yamlMapper.readValue(rayJob.getTrainingConfig(), TrainingConfigDTO.class);
            var mergeLora = rayJob.getEnvVars().stream()
                .filter(envVar -> "MERGE_LORA".equals(envVar.getKey()))
                .filter(envVar -> "true".equals(envVar.getValue()))
                .findAny()
                .map(envVar -> Boolean.TRUE)
                .orElse(Boolean.FALSE);

            return Optional.ofNullable(trainingConfig.getLora()).orElse(Boolean.FALSE) && !mergeLora;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Boolean.TRUE;
        }
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
                StringUtils.isEmpty(rayJob.getDeployedNamespace()) ? "" : ("." + rayJob.getDeployedNamespace()),
                ":",
                VLLM_CONTROLLER_PORT.toString()
            ) :
            StringUtils.join("https://", rayJob.getChatHostName());
    }

    /**
     * Process VLLM chat request via WebSocket
     */
    public void sendVllmMessage(VllmChatRequestDTO request) {
        String applicationId = request.getApplicationId();
        String topic = "/topic/message/" + applicationId;

        vllmActiveStreams.put(applicationId, true);
        Application app = applicationRepository.findById(request.getApplicationId()).orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));

        try {
            String url = chatVllmUrl(request, app) + CHAT_ENDPOINT;
            String modelName = extractAppModelName(app);
            log.debug("sendVllmMessage() - modelName: {}", modelName);
            request.setModel(modelName);
            VLLMMessage vllmRequest = buildVllmRequest(request);
            streamVllm(url, vllmRequest, applicationId, topic);

        } catch (Exception e) {
            log.error("Failed to send VLLM chat message for application {}", applicationId, e);
            sendVllmError(topic, applicationId, e.getMessage());
        } finally {
            vllmActiveStreams.remove(applicationId);
        }
    }

    private String chatVllmUrl(VllmChatRequestDTO request, Application app){

        return Optional.ofNullable(applicationProperties.getK8sAccessMode()).orElse(Boolean.FALSE) ?
            StringUtils.join(
                "http://", request.getInternalEndpoint()
            ) :
            StringUtils.join("https://", app.getIngressHostName());
    }

    /**
     * Abort an active VLLM stream
     */
    public void abortVllmStream(String applicationId) {
        vllmActiveStreams.put(applicationId, false);
        String topic = "/topic/message/" + applicationId;
        try {
            VllmChatResponseDTO response = VllmChatResponseDTO.builder()
                .applicationId(applicationId)
                .payload("[ABORTED]")
                .build();
            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Failed to send abort confirmation", e);
        }
    }

    /**
     * Build VLLM request from frontend DTO
     */
    private VLLMMessage buildVllmRequest(VllmChatRequestDTO request) {
        VLLMMessage.VLLMMessageBuilder builder = VLLMMessage.builder()
            .model(request.getModel())
            .messages(request.getMessages())
            .streamOptions(Map.of("include_usage", true))
            .stream(true);
        if (Boolean.TRUE.equals(request.getEnableThinking())) {
            builder.chatTemplateKwargs(
                VLLMMessage.ChatTemplateKwArgs.builder()
                    .enableThinking(true)
                    .build()
            );
        }else {
            builder.chatTemplateKwargs(
                VLLMMessage.ChatTemplateKwArgs.builder()
                    .enableThinking(false)
                    .build()
            );
        }


        if (request.getAdvancedParams() != null) {
            Map<String, Object> params = request.getAdvancedParams();
            if (params.containsKey("temperature")) {
                builder.temperature(((Number) params.get("temperature")).doubleValue());
            }
            if (params.containsKey("top_p")) {
                builder.topP(((Number) params.get("top_p")).doubleValue());
            }
            if (params.containsKey("top_k")) {
                builder.topK(((Number) params.get("top_k")).intValue());
            }
            if (params.containsKey("max_tokens")) {
                builder.maxTokens(((Number) params.get("max_tokens")).intValue());
            }
        }



        return builder.build();
    }

    /**
     * Stream VLLM response (similar to existing stream() method)
     */
    private void streamVllm(String url, VLLMMessage message, String applicationId, String topic) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON));

        try {
            byte[] json = objectMapper.writeValueAsBytes(message);
            headers.setContentLength(json.length);

            vllmRestTemplate.execute(
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
                        processVllmStream(reader, applicationId, topic);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                }
            );
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Process VLLM SSE stream
     */
    private void processVllmStream(BufferedReader reader, String applicationId, String topic) throws Exception {
        String line;
        boolean isInThinking = false;
        Integer inputTokens = null;
        Integer outputTokens = null;
        Integer totalTokens = null;
        String finishReason = null;

        while ((line = reader.readLine()) != null) {
            if (!vllmActiveStreams.getOrDefault(applicationId, false)) {
                log.info("VLLM stream aborted for {}", applicationId);
                break;
            }

            line = line.trim();

            if (!line.startsWith("data:")) continue;
            String payload = line.substring(5).trim();
            if ("[DONE]".equals(payload)) {
                sendVllmDone(topic, applicationId, finishReason, inputTokens, outputTokens, totalTokens);
                break;
            }

            try {
                JsonNode root = objectMapper.readTree(payload);
                JsonNode choice = root.path("choices").path(0);
                String content = choice.path("delta").path("content").asText(null);

                if (content != null && !content.isEmpty()) {
                    if (content.contains("<think>")) {
                        isInThinking = true;
                        String[] parts = content.split("<think>", 2);
                        if (!parts[0].isEmpty()) {
                            sendVllmChunk(topic, applicationId, parts[0], false);
                        }
                        if (parts.length > 1) {
                            sendVllmChunk(topic, applicationId, parts[1], true);
                        }
                    } else if (content.contains("</think>")) {
                        isInThinking = false;
                        String[] parts = content.split("</think>", 2);
                        if (!parts[0].isEmpty()) {
                            sendVllmChunk(topic, applicationId, parts[0], true);
                        }
                        if (parts.length > 1) {
                            sendVllmChunk(topic, applicationId, parts[1], false);
                        }
                    } else {
                        sendVllmChunk(topic, applicationId, content, isInThinking);
                    }
                }

                String currentFinish = choice.path("finish_reason").asText(null);
                if (currentFinish != null && !"null".equals(currentFinish)) {
                    finishReason = currentFinish;
                }

                JsonNode usage = root.path("usage");
                if (!usage.isMissingNode()) {
                    inputTokens = usage.path("prompt_tokens").asInt(0);
                    outputTokens = usage.path("completion_tokens").asInt(0);
                    totalTokens = usage.path("total_tokens").asInt(0);
                }
            } catch (Exception e) {
                log.warn("Failed to parse VLLM SSE line: {}", payload);
            }
        }
    }

    /**
     * Send chunk to VLLM WebSocket topic
     */
    private void sendVllmChunk(String topic, String applicationId, String content, boolean isThinking) {
        try {
            VllmChatResponseDTO response = VllmChatResponseDTO.builder()
                .applicationId(applicationId)
                .payload(content)
                .isThinking(isThinking)
                .build();

            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Failed to send VLLM chunk for {}", applicationId, e);
        }
    }

    /**
     * Send [DONE] with metadata
     */
    private void sendVllmDone(String topic, String applicationId, String finishReason,
                              Integer inputTokens, Integer outputTokens, Integer totalTokens) {
        try {
            UsageDTO usage = null;
            if (inputTokens != null || outputTokens != null || totalTokens != null) {
                usage = UsageDTO.builder()
                    .inputTokens(inputTokens)
                    .outputTokens(outputTokens)
                    .totalTokens(totalTokens)
                    .build();
            }

            MessageInfoDTO info = MessageInfoDTO.builder()
                .finishReason(finishReason != null ? finishReason : "stop")
                .usage(usage)
                .provider("vllm")
                .build();

            VllmChatResponseDTO response = VllmChatResponseDTO.builder()
                .applicationId(applicationId)
                .payload("[DONE]")
                .info(info)
                .build();

            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Failed to send VLLM [DONE] for {}", applicationId, e);
        }
    }

    /**
     * Send error to VLLM WebSocket topic
     */
    private void sendVllmError(String topic, String applicationId, String errorMessage) {
        try {
            VllmChatResponseDTO response = VllmChatResponseDTO.builder()
                .applicationId(applicationId)
                .error(errorMessage)
                .build();

            messagingTemplate.convertAndSend(topic, objectMapper.writeValueAsString(response));
        } catch (Exception e) {
            log.error("Failed to send VLLM error for {}", applicationId, e);
        }
    }

    private String extractAppModelName(Application application) {
        if (application == null || application.getExtraConfig() == null) {
            return null;
        }

        try {
            JsonNode config = objectMapper.readTree(application.getExtraConfig());

            if (config.has("loraSourceModel") && !config.get("loraSourceModel").isNull()) {
                String loraModel = config.get("loraSourceModel").asText();
                if (!loraModel.isEmpty()) {
                    return "serve-lora";  // Fixed name for LoRA adapters
                }
            }

            String source = config.has("source") ? config.get("source").asText() : null;

            if ("hub".equals(source)) {
                if (config.has("branchToDeploy") && !config.get("branchToDeploy").isNull()) {
                    return "/models/" + config.get("branchToDeploy").asText();
                }
            }

            if ("hf".equals(source)) {
                return config.has("hfModelName") ? config.get("hfModelName").asText() : null;
            }

            if (config.has("branchToDeploy") && !config.get("branchToDeploy").isNull()) {
                return config.get("branchToDeploy").asText();
            }

            if (config.has("modelName")) {
                return config.get("modelName").asText();
            }

            return null;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse extraConfig for application {}", application.getId(), e);
            return null;
        }
    }
}
