package net.statemesh.service.dto.vllm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageDTO {
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
}
