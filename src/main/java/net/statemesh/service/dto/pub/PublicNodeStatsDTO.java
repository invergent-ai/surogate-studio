package net.statemesh.service.dto.pub;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PublicNodeStatsDTO implements Serializable {
    String zone;
    Integer nodes;
    Long cpu;
    Long memory;
}
