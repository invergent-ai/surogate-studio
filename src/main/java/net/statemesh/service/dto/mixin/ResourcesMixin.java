package net.statemesh.service.dto.mixin;

import com.fasterxml.jackson.annotation.JsonProperty;
import net.statemesh.service.dto.SkyConfigDTO;

import java.util.List;

public abstract class ResourcesMixin {
    @JsonProperty("accelerator_args")
    private String acceleratorArgs;

    @JsonProperty("instance_type")
    private String instanceType;

    @JsonProperty("use_spot")
    private Boolean useSpot;

    @JsonProperty("disk_size")
    private Integer diskSize;

    @JsonProperty("disk_tier")
    private String diskTier;

    @JsonProperty("network_tier")
    private String networkTier;

    @JsonProperty("image_id")
    private String imageId;

    @JsonProperty("any_of")
    private List<SkyConfigDTO.Resources.AnyOf> anyOf;

    @JsonProperty("job_recovery")
    private String jobRecovery;
}
