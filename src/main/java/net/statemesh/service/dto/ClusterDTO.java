package net.statemesh.service.dto;

import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.Cluster} entity.
 */
@Data
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ClusterDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @NotNull
    private String cid;

    @Lob
    private String kubeConfig;

    private String prometheusUrl;
    private String redisUrl;
    private Double requestVsLimitsCoefficientCpu;
    private Double requestVsLimitsCoefficientMemory;

    @Size(max = 200)
    private String description;

    @NotNull
    private ZoneDTO zone;

    public ClusterDTO zone(ZoneDTO zone) {
        this.zone = zone;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClusterDTO clusterDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, clusterDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
