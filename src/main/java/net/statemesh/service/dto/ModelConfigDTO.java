package net.statemesh.service.dto;

import lombok.Data;
import net.statemesh.domain.enumeration.ModelDeploymentMode;
import net.statemesh.domain.enumeration.ModelRoutingStrategy;

import java.util.Map;

@Data
public class ModelConfigDTO {
    String modelName;
    Integer maxContextSize;

    Boolean enablePartitioning;
    Integer partitions;

    Boolean enableReplication;
    Integer replicas;

    ModelRoutingStrategy routingStrategy;

    Integer routerReplicas;
    String routerSessionKey;

    ModelDeploymentMode deploymentMode;

    Boolean l1Cache;
    Integer l1CacheSize; // in GB
    Boolean l2Cache;
    Integer l2CacheSize; // in GB

    Long gpuMemory; // in MB

    Map<String, Object> hfConfig;

    // https://huggingface.co/api/models/{model}.['safetensors']['total']
    Long hfTotalSafetensors;

    private String source; // hub vs hf
    private String hfToken;
    private String hfModelName;
    private String branchToDeploy;
    private String branchToDeployDisplayName;
    private String loraSourceModel;

    public boolean cachingEnabled() {
        return Boolean.TRUE.equals(l1Cache) || Boolean.TRUE.equals(l2Cache);
    }
}
