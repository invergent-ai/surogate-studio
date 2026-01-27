package net.statemesh.service.dto.vllm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageInfoDTO {
    private String finishReason;
    private String model;
    private UsageDTO usage;
    private String provider;
}
