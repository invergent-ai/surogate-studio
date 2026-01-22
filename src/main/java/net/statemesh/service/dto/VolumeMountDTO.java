package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.VolumeMount} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"volume", "database"})
public class VolumeMountDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 255)
    private String containerPath;

    private Boolean readOnly;

    private VolumeDTO volume;

    @JsonIgnore
    private ContainerDTO container;


    @JsonIgnore
    private DatabaseDTO database;

    @Transient
    // used in net.statemesh.service.mapper.VolumeMountMapper
    private String applicationId;

    @Transient
    // used in net.statemesh.service.mapper.VolumeMountMapper
    private String databaseId;

    @Transient
    // used in net.statemesh.service.mapper.VolumeMountMapper
    private String virtualMachineId;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VolumeMountDTO volumeMountDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, volumeMountDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
