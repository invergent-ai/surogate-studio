package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.DatabaseStatus;
import net.statemesh.k8s.util.ResourceStatus;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "database")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"project", "volumeMounts"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Database implements Resource, Serializable {
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

    @Column(name = "internal_name")
    private String internalName;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "has_ingress")
    private Boolean hasIngress;

    @Column(name = "ingress_host_name")
    private String ingressHostName;

    @Column(name = "deployed_namespace")
    private String deployedNamespace;

    @Column(name = "error_message_key")
    private String errorMessageKey;

    @Column(name = "warn_message_key")
    private String warnMessageKey;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DatabaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage")
    private ResourceStatus.ResourceStatusStage stage;

    @Min(value = 0)
    @Max(value = 100)
    @Column(name = "cpu_limit")
    private Double cpuLimit;

    @Column(name = "mem_limit")
    private String memLimit;

    @NotNull
    @Min(value = 1)
    @Max(value = 16)
    @Column(name = "replicas", nullable = false)
    private Integer replicas;

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @NotNull
    @JsonIgnoreProperties(value = { "user", "organization", "cluster", "applications", "databases", "virtualMachines", "accessLists" }, allowSetters = true)
    private Project project;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "database")
    @JsonIgnoreProperties(value = {"volume", "database"}, allowSetters = true)
    @Builder.Default
    private Set<VolumeMount> volumeMounts = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "database")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"database"}, allowSetters = true)
    @Builder.Default
    private Set<FirewallEntry> firewallEntries = new HashSet<>();

    public void setVolumeMounts(Set<VolumeMount> volumeMounts) {
        if (this.volumeMounts != null) {
            this.volumeMounts.forEach(i -> i.setDatabase(null));
        }
        if (volumeMounts != null) {
            volumeMounts.forEach(i -> i.setDatabase(this));
        }
        this.volumeMounts = volumeMounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Database)) {
            return false;
        }
        return getId() != null && getId().equals(((Database) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
