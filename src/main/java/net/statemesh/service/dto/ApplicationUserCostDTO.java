package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationUserCostDTO {
    private String userId;
    private List<ApplicationCostMetrics> applications;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApplicationCostMetrics {
        private String appName;
        private String namespace;
        private BigDecimal totalCost;
        private BigDecimal cpuCost;
        private BigDecimal ramCost;
        private BigDecimal gpuCost;
        private BigDecimal pvCost;
        private String payeeId;
    }
}
