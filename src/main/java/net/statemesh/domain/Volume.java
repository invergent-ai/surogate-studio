package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.VolumeType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Volume.
 */
@Entity
@Table(name = "volume")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"project"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Volume implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Size(max = 50)
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Size(max = 255)
    @Column(name = "path")
    private String path;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VolumeType type;

    @Column(name = "size")
    private Integer size;

    @Column(name = "bucket_url")
    private String bucketUrl;

    @Column(name = "access_key")
    private String accessKey;

    @Column(name = "region")
    private String region;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = {"applications", "virtualMachines", "accessLists"}, allowSetters = true)
    @NotNull
    @JoinColumn(name = "owning_project_id")
    private Project project;

    @OneToMany(mappedBy = "volume")
    @JsonIgnoreProperties(value = {"volume", "container", "virtualMachine"}, allowSetters = true)
    @Builder.Default
    private Set<VolumeMount> mounts = new HashSet<>();

    public String getProjectId() {
        return project != null ? project.getId() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Volume)) {
            return false;
        }
        return getId() != null && getId().equals(((Volume) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
