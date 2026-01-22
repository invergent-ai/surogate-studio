package net.statemesh.service.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.UserXOrganization} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
@Data
public class UserXOrganizationDTO implements Serializable {

    private String id;

    private UserDTO user;

    private OrganizationDTO organization;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserXOrganizationDTO userXOrganizationDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, userXOrganizationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
