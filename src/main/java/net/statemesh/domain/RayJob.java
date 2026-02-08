package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.RayJobProvisioningStatus;
import net.statemesh.domain.enumeration.RayJobType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "ray_job")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"envVars", "project"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class RayJob extends AbstractAuditingEntity<String> implements Resource, Serializable {
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

    private String description;

    @Column(name = "internal_name")
    private String internalName;

    @Column(name = "work_dir_volume_name")
    private String workDirVolumeName;

    @Column(name = "job_id")
    private String jobId;

    @Column(name = "submission_id")
    private String submissionId;

    @Column(name = "deployed_namespace")
    private String deployedNamespace;

    @Column(name = "chat_host_name")
    private String chatHostName;

    @Column(name = "pod_name")
    private String podName;

    @Column(name = "container")
    private String container;

    @Column(name = "completed_status")
    private String completedStatus;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private RayJobType type;

    @Column(name = "run_in_the_sky")
    private Boolean runInTheSky;

    @Column(name = "sky_to_k8s")
    private Boolean skyToK8s;

    @Column(name = "use_axolotl")
    private Boolean useAxolotl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provisioning_status", nullable = false)
    private RayJobProvisioningStatus provisioningStatus;

    @Column(name = "training_config", columnDefinition = "text")
    private String trainingConfig;

    @Column(name = "ray_cluster_shape", columnDefinition = "text")
    private String rayClusterShape;

    @Column(name = "sky_config", columnDefinition = "text")
    private String skyConfig;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "job")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"job"}, allowSetters = true)
    @Builder.Default
    private Set<JobEnvironmentVariable> envVars = new HashSet<>();

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @NotNull
    @JsonIgnoreProperties(value = { "user", "organization", "cluster", "applications", "databases", "virtualMachines", "accessLists", "jobs" }, allowSetters = true)
    private Project project;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof RayJob)) {
            return false;
        }
        return getId() != null && getId().equals(((RayJob) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
