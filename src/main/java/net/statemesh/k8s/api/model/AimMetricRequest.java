package net.statemesh.k8s.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AimMetricRequest {
    private String name;
    @JsonProperty("last_value")
    private Double lastValue;
    private AimContext context;
}
