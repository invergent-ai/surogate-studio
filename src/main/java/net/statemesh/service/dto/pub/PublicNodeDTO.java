package net.statemesh.service.dto.pub;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;

@Data
public class PublicNodeDTO implements Serializable {
    private Double hourlyPrice;
    private Double totalNodeEarnings;
    private Instant creationTime;
    private PublicNodeResourceDTO resource;
    private PublicClusterDTO cluster;
}
