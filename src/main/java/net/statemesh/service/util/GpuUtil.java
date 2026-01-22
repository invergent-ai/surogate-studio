package net.statemesh.service.util;

import net.statemesh.service.dto.NodeStatsDTO;

public class GpuUtil {
    public static int[] availableGpuMemoryFromNodeStats(NodeStatsDTO nodeStats) {
        int[] availableGpuMemory = new int[nodeStats.getGpuCount()];
        for (var i = 0; i < nodeStats.getGpuCount(); i++) {
            availableGpuMemory[i] = nodeStats.getGpuMemoryFree().get(String.valueOf(i));
        }
        return availableGpuMemory;
    }
}
