package net.statemesh.service.dto.vllm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VllmChatResponseDTO {
    private String applicationId;
    private String payload;           // Content chunk or "[DONE]"
    private Boolean isThinking;       // Is this part of <think> tags?
    private String error;             // Error message if any
    private MessageInfoDTO info;      // Final metadata (sent with last chunk or [DONE])
}
