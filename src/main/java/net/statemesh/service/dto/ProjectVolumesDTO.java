package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class ProjectVolumesDTO {
    private String projectId;
    private String projectName;
    private Set<VolumeWithMountsDTO> volumes;

}
