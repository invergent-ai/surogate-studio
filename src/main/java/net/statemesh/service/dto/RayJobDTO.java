package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.statemesh.domain.enumeration.RayJobProvisioningStatus;
import net.statemesh.domain.enumeration.RayJobType;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RayJobDTO implements ResourceDTO, Serializable {

    private String id;

    @NotNull
    @Size(max = 50)
    private String name;
    private String description;
    private String internalName;
    private String workDirVolumeName;
    private String jobId;
    private String submissionId;
    private String deployedNamespace;
    private String chatHostName;
    private String podName;
    private String container;
    private String completedStatus;

    private Instant createdDate;
    private Instant startTime;
    private Instant endTime;

    @NotNull
    private RayJobType type;
    private Boolean runInTheSky;
    private Boolean skyToK8s;
    private Boolean useAxolotl;

    @NotNull
    private RayJobProvisioningStatus provisioningStatus;

    private String trainingConfig;
    private String rayClusterShape;
    private String skyConfig;
    private String kubeConfig;

    @Builder.Default
    private Set<JobEnvironmentVariableDTO> envVars = new HashSet<>();

    @NotNull
    private ProjectDTO project;

    // Transient
    private TrainingConfigDTO trainingConfigPojo;
    private RayClusterShape rayClusterShapePojo;
    private SkyConfigDTO skyConfigPojo;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RayJobDTO rayJobDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, rayJobDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String getStatusValue() {
        return null;
    }

    @Override
    public Set<ResourceAllocationDTO> resourceAllocations() {
        return null;
    }

    @Override
    public boolean isKeepVolumes() {
        return false;
    }
}
