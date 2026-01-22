package net.statemesh.service.dto;

import java.util.Set;

public interface ResourceDTO {
    String getId();
    String getName();
    String getInternalName();
    String getDeployedNamespace();
    ProjectDTO getProject();
    void setProject(ProjectDTO project);
    String getStatusValue();
    Set<ResourceAllocationDTO> resourceAllocations();
    boolean isKeepVolumes();
}
