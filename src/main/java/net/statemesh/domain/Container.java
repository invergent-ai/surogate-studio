package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.statemesh.domain.enumeration.ContainerType;
import net.statemesh.domain.enumeration.PullImageMode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Container.
 */
@Entity
@Table(name = "container")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"application", "volumeMounts", "ports", "envVars"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Container implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Column(name = "image_name", nullable = false)
    private String imageName;

    @NotNull
    @Column(name = "image_tag", columnDefinition = "varchar(100) default 'latest'", nullable = false)
    private String imageTag;

    @Column(name = "display_name")
    private String displayName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ContainerType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "pull_image_mode", nullable = false)
    private PullImageMode pullImageMode;

    @Column(name = "registry_url")
    private String registryUrl;

    @Column(name = "registry_username")
    private String registryUsername;

    @Min(value = 0)
    @Max(value = 100)
    @Column(name = "cpu_request")
    @Deprecated // Computed programmatically: coefficient * cpuLimit
    private Double cpuRequest;

    @Min(value = 0)
    @Max(value = 100)
    @Column(name = "cpu_limit")
    private Double cpuLimit;

    @Column(name = "mem_request")
    @Deprecated // Computed programmatically: coefficient * memLimit
    private String memRequest;

    @Column(name = "mem_limit")
    private String memLimit;

    @Min(value = 0)
    @Max(value = 64)
    @Column(name = "gpu_limit")
    private Integer gpuLimit;

    @Column(name = "start_command")
    private String startCommand;

    @Column(name = "start_parameters", columnDefinition = "text")
    private String startParameters;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = {"annotations", "labels", "containers", "volumes"}, allowSetters = true)
    private Application application;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "container")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"container"}, allowSetters = true)
    @Builder.Default
    private Set<EnvironmentVariable> envVars = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "container")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"container"}, allowSetters = true)
    @Builder.Default
    private Set<FirewallEntry> firewallEntries = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "container")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"protocol", "container"}, allowSetters = true)
    @Builder.Default
    private Set<Port> ports = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "container")
    @JsonIgnoreProperties(value = {"volume", "container"}, allowSetters = true)
    @Builder.Default
    private Set<VolumeMount> volumeMounts = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "container")
    @JsonIgnoreProperties(value = {"container"}, allowSetters = true)
    @Builder.Default
    private Set<Probe> probes = new HashSet<>();

    public void setEnvVars(Set<EnvironmentVariable> environmentVariables) {
        if (this.envVars != null) {
            this.envVars.forEach(i -> i.setContainer(null));
        }
        if (environmentVariables != null) {
            environmentVariables.forEach(i -> i.setContainer(this));
        }
        this.envVars = environmentVariables;
    }

    public void setPorts(Set<Port> ports) {
        if (this.ports != null) {
            this.ports.forEach(i -> i.setContainer(null));
        }
        if (ports != null) {
            ports.forEach(i -> i.setContainer(this));
        }
        this.ports = ports;
    }

    public void setVolumeMounts(Set<VolumeMount> volumeMounts) {
        if (this.volumeMounts != null) {
            this.volumeMounts.forEach(i -> i.setContainer(null));
        }
        if (volumeMounts != null) {
            volumeMounts.forEach(i -> i.setContainer(this));
        }
        this.volumeMounts = volumeMounts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Container)) {
            return false;
        }
        return getId() != null && getId().equals(((Container) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
