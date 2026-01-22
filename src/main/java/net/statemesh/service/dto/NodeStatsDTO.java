package net.statemesh.service.dto;

import lombok.Builder;
import lombok.Data;
import net.statemesh.domain.enumeration.NodeStatus;

import java.util.Map;

@Data
@Builder
public class NodeStatsDTO {
    private Integer rxMbps;
    private Integer txMbps;
    private Integer activeApps;
    private Integer totalApps;
    private NodeStatus status;
    private Long uptime;
    private Integer gpuCount;
    private String gpuModel;
    private Map<String, Integer> gpuMemory; // in Mb
    private Map<String, Integer> gpuUsage;
    private Map<String, Integer> gpuMemoryUsage;
    private Map<String, Integer> gpuMemoryFree;
    private Map<String, Integer> gpuTemperature;
    private Map<String, Integer> gpuPowerUsage;
}
