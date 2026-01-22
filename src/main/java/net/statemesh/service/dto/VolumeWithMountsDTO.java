package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.statemesh.domain.enumeration.VolumeType;

import java.util.Set;

@Data
@AllArgsConstructor
public class VolumeWithMountsDTO {
    private String id;
    private String name;
    private String path;
    private VolumeType type;
    private Integer size;
    private Set<VolumeMountStringsDTO> volumeMounts;
}
