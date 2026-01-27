package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VLLMMessage implements Serializable {
    private String model;
    private Double temperature;
    @JsonProperty("top_p")
    private Double topP;
    @JsonProperty("top_k")
    private Integer topK;
    @JsonProperty("min_p")
    private Double minP;
    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("max_tokens")
    private Integer maxTokens;
    private Boolean stream;
    @JsonProperty("chat_template_kwargs")
    private ChatTemplateKwArgs chatTemplateKwargs;

    @JsonProperty("stream_options")
    private Map<String, Object> streamOptions;

    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @Data
    @Builder
    public static class Message {
        private String role;
        private Object content;
    }

    @Data
    @Builder
    public static class ChatTemplateKwArgs {
        @JsonProperty("enable_thinking")
        private Boolean enableThinking;
    }
}
