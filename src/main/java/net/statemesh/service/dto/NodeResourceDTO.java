package net.statemesh.service.dto;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NodeResourceDTO implements Serializable {
    private String id;
    private BigDecimal rxMbps;
    private BigDecimal txMbps;
    private BigDecimal allocatableCpu;
    private BigDecimal allocatableMemory;
    private BigDecimal allocatableEphemeralStorage;
    private BigDecimal capacityCpu;
    private BigDecimal capacityMemory;
    private BigDecimal capacityEphemeralStorage;
}
