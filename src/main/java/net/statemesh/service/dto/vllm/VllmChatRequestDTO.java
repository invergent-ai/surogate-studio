package net.statemesh.service.dto.vllm;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VllmChatRequestDTO {
    private String applicationId;
    private String baseUrl;
    private String model;
    private List<ChatMessageDTO> messages;
    private Map<String, Object> advancedParams;
    private String systemPrompt;
    private Boolean enableThinking;
    private String internalEndpoint;
}
