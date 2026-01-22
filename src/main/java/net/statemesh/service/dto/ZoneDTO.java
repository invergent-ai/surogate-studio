package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.Zone} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
@Data
public class ZoneDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @NotNull
    private String zoneId;

    @NotNull
    private String vpnApiKey;

    @NotNull
    private String iperfIp;

    private OrganizationDTO organization;

    @Transient
    private Boolean hasHPC;
    @Transient
    private Boolean hasGPU;

    public ZoneDTO zoneId(String zoneId) {
        this.zoneId = zoneId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ZoneDTO zoneDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, zoneDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
