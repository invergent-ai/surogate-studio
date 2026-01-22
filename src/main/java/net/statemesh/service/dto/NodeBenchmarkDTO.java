package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Getter
@Setter
public class NodeBenchmarkDTO implements Serializable {
    private String id;

    @NotNull
    private Instant created;
    private Instant updated;

    Integer rxMbps;
    Integer txMbps;
    Integer cpuLogicalCores;
    String publicIp;
    String cpuFeatures;
}
