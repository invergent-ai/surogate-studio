package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectResourceDTO {
    String id;
    String name;
    ProjectResourceType type;

    public enum ProjectResourceType {
        APPLICATION,
        VIRTUAL_INSTANCE,
        VOLUME,
        DATABASE
    }
}
