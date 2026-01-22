package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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

    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    @Builder
    public static class ChatTemplateKwArgs {
        @JsonProperty("enable_thinking")
        private Boolean enableThinking;
    }
}
