package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.statemesh.domain.enumeration.VolumeType;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.Volume} entity.
 */
@Data
@SuppressWarnings("common-java:DuplicatedBlocks")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VolumeDTO implements Serializable {
    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 255)
    private String path;

    @NotNull
    private VolumeType type;

    private Integer size;

    private String bucketUrl;
    private String accessKey;
    private String region;

    private ProjectDTO project;

    // Transient
    private String accessSecret;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VolumeDTO volumeDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.name, volumeDTO.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }
}
