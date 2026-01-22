package net.statemesh.service.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Builder
@Data
public class MessageDTO implements Serializable {
    private List<ApplicationDTO> apps;
    private List<DatabaseDTO> dbs;
    private List<NodeDTO> nodes;
    private List<VolumeDTO> volumes;
    private List<ProjectDTO> projects;
    private List<RayJobDTO> jobs;
    private List<TaskRunDTO> tasks;
    private MessageType type;

    public enum MessageType {
        CREATE,
        UPDATE,
        DELETE
    }
}
