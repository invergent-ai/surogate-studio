package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.statemesh.domain.Project;
import net.statemesh.domain.enumeration.Profile;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.Project} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 50)
    private String alias;

    @Size(max = 200)
    private String description;

    private String namespace;

    private String datacenterName;
    private String rayCluster;
    private Profile profile;

    private Boolean deleted;

    private UserDTO user;

    private OrganizationDTO organization;

    private ClusterDTO cluster;

    @NotNull
    private ZoneDTO zone;

    public ProjectDTO namespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public ProjectDTO cluster(ClusterDTO cluster) {
        this.cluster = cluster;
        return this;
    }

    public ProjectDTO zone(ZoneDTO zone) {
        this.zone = zone;
        return this;
    }

    public ProjectDTO organization(OrganizationDTO organization) {
        this.organization = organization;
        return this;
    }

    public ProjectDTO datacenterName(String datacenterName) {
        this.datacenterName = datacenterName;
        return this;
    }

    public ProjectDTO rayCluster(String rayCluster) {
        this.rayCluster = rayCluster;
        return this;
    }

    public ProjectId idProjection() {
        return ProjectId.builder().id(getId()).build();
    }

    public static ProjectDTO basicProjection(Project project) {
        return ProjectDTO.builder()
            .id(project.getId())
            .name(project.getName())
            .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectDTO projectDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, projectDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
