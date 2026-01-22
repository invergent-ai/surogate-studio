package net.statemesh.service.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResourceAllocationDTO {
    private Integer replicas;
    private Double cpuLimit;
    private String memLimit;
}
