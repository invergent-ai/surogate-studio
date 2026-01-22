package net.statemesh.k8s.api.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AimContext {
    private String phase;
}
