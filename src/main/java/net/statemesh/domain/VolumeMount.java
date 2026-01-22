package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * A VolumeMount.
 */
@Entity
@Table(name = "volume_mount")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"volume", "container"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class VolumeMount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Size(max = 255)
    @Column(name = "container_path", nullable = false)
    private String containerPath;

    @Column(name = "read_only")
    private Boolean readOnly;

    @ManyToOne(optional = false, cascade = CascadeType.ALL)
    @NotNull
    @JsonIgnoreProperties(value = { "project" }, allowSetters = true)
    private Volume volume;

    @ManyToOne
    @JsonIgnoreProperties(value = { "application", "envVars", "ports", "volumes" }, allowSetters = true)
    private Container container;

    @ManyToOne
    @JsonIgnoreProperties(allowSetters = true)
    private Database database;

    public VolumeMount detachVolume() {
        this.volume = null;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VolumeMount)) {
            return false;
        }
        return getId() != null && getId().equals(((VolumeMount) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
