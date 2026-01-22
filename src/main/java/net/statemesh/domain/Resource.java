package net.statemesh.domain;

public interface Resource {
    String getId();
    String getName();
    String getInternalName();
    String getDeployedNamespace();
    Project getProject();
    void setProject(Project project);
}
