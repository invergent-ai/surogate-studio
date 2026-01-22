package net.statemesh.service.dto.pub;

import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicNodeResourceDTO implements Serializable {
    private BigDecimal capacityCpu;
    private BigDecimal capacityMemory;
    private BigDecimal capacityEphemeralStorage;
}
