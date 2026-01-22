package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.statemesh.domain.enumeration.TaskRunProvisioningStatus;
import net.statemesh.domain.enumeration.TaskRunType;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TaskRunDTO implements ResourceDTO, Serializable {

    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    private String internalName;
    private String deployedNamespace;
    private Instant startTime;
    private Instant endTime;
    private String podName;
    private String container;
    private String completedStatus;

    @NotNull
    private TaskRunType type;

    @NotNull
    private TaskRunProvisioningStatus provisioningStatus;

    private Instant createdDate;

    @Builder.Default
    private Set<TaskRunParamDTO> params = new HashSet<>();

    @NotNull
    private ProjectDTO project;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaskRunDTO dto)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, dto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String getStatusValue() {
        return Optional.ofNullable(provisioningStatus).map(Enum::name).orElse(null);
    }

    @Override
    public Set<ResourceAllocationDTO> resourceAllocations() {
        return Collections.emptySet();
    }

    @Override
    public boolean isKeepVolumes() {
        return false;
    }
}
