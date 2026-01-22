package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RayClusterShape {
    private Integer numNodes;
    private Integer gpusPerWorker;

    private Integer headGpus; // (Optional) Must be set if test vllm chat is needed or useHeadAsWorker = true
    private Boolean useHeadAsWorker; // (Optional) Can be true only if numNodes > 1 and headGpus >= gpusPerWorker
    private Integer testVllmTp; // (Optional) Must be set if test vllm chat is needed. Must be <= headGpus
}
