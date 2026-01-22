package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VolumeMountStringsDTO {
    private String id;
    private String containerPath;
    private Boolean readOnly;
    private String volumeId;
    private String containerId;
    private String virtualMachineId;
}
