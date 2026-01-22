package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import net.statemesh.domain.enumeration.OrganizationType;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.Organization} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
@Data
public class OrganizationDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    private OrganizationType type;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OrganizationDTO organizationDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, organizationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
