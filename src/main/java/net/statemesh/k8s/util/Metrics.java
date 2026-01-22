package net.statemesh.k8s.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Getter
@ToString
@AllArgsConstructor
@Builder
public class Metrics {
    private Map<Double, BigDecimal> cpu;
    private Map<Double, BigDecimal> memory;
    private Map<Double, BigDecimal> networkIn;
    private Map<Double, BigDecimal> networkOut;
    private Integer gpuCount;
    private String gpuModel;
    private Map<String, Map<Double, BigDecimal>> gpuMemory;
    private Map<String, Map<Double, BigDecimal>> gpuUsage;
    private Map<String, Map<Double, BigDecimal>> gpuMemoryUsage;
    private Map<String, Map<Double, BigDecimal>> gpuMemoryFree;
    private Map<String, Map<Double, BigDecimal>> gpuTemperature;
    private Map<String, Map<Double, BigDecimal>> gpuPowerUsage;

    private Map<Double, BigDecimal> modelRequestsRunning;
    private Map<Double, BigDecimal> modelRequestsWaiting;
    private Map<Double, BigDecimal> modelKvCacheUsage;
    private Map<Double, BigDecimal> modelPromptTokensRate;
    private Map<Double, BigDecimal> modelGenerationTokensRate;
    private Map<Double, BigDecimal> modelTimeToFirstToken;
    private Map<Double, BigDecimal> modelTimePerOutputToken;
    private Map<Double, BigDecimal> modelPrefillTime;
    private Map<Double, BigDecimal> modelDecodeTime;
    private Map<Double, BigDecimal> modelSwappedRequests;
    private Map<Double, BigDecimal> modelCacheHitRate;
    private Map<Double, BigDecimal> modelCacheHitsTotal;
    private Map<Double, BigDecimal> modelCacheQueriesTotal;
    private Map<Double, BigDecimal> modelHealthyPods;
    private Map<Double, BigDecimal> modelCurrentQps;

    private Map<Double, BigDecimal> modelPreemptionsTotal;
    private Map<Double, BigDecimal> modelIterationTokensTotal;
    private Map<Double, BigDecimal> modelE2eLatency;
    private Map<Double, BigDecimal> modelQueueTime;
    private Map<Double, BigDecimal> modelInferenceTime;
    private Map<Double, BigDecimal> modelRequestSuccessTotal;

    private Map<Double, BigDecimal> routerRequestsTotal;
    private Map<Double, BigDecimal> routerRequestsDuration;
    private Map<Double, BigDecimal> routerActiveConnections;
    private Map<Double, BigDecimal> routerWorkerHealth;
    private Map<Double, BigDecimal> routerWorkerLoad;
    private Map<Double, BigDecimal> routerRoutingDecisions;
    private Map<Double, BigDecimal> routerErrorRate;
    private Map<Double, BigDecimal> routerTimeoutRate;
    private Map<Double, BigDecimal> routerCpuUsage;
    private Map<Double, BigDecimal> routerMemoryUsage;
    private Map<Double, BigDecimal> routerDiskUsage;

    // Training metrics (Aim)
    private Map<Double, BigDecimal> epoch;
    private Map<Double, BigDecimal> gradNorm;
    private Map<Double, BigDecimal> learningRate;
    private Map<Double, BigDecimal> loss;
    private Map<Double, BigDecimal> evalLoss;
    private Map<Double, BigDecimal> tokensPerSecondPerGpu;

    private Instant created;
}
