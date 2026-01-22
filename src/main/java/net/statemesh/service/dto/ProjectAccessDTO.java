package net.statemesh.service.dto;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.util.Objects;

import lombok.Data;
import net.statemesh.domain.enumeration.ProjectAccessRole;

/**
 * A DTO for the {@link net.statemesh.domain.ProjectAccess} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
@Data
public class ProjectAccessDTO implements Serializable {

    private String id;

    @NotNull
    private ProjectAccessRole role;

    private UserDTO user;

    private ProjectDTO project;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectAccessDTO projectAccessDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, projectAccessDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
