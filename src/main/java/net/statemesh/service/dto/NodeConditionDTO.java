package net.statemesh.service.dto;

import lombok.*;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NodeConditionDTO implements Serializable {
    private String id;
    private Boolean memoryPressure;
    private Boolean diskPressure;
    private Boolean pidPressure;
    private Boolean kubeletNotReady;
}
