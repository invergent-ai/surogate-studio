package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.TaskRunProvisioningStatus;
import net.statemesh.domain.enumeration.TaskRunType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "task_run")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"params", "project"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class TaskRun extends AbstractAuditingEntity<String> implements Resource, Serializable  {
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

    @Column(name = "deployed_namespace")
    private String deployedNamespace;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private TaskRunType type;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "pod_name")
    private String podName;

    @Column(name = "container")
    private String container;

    @Column(name = "completed_status")
    private String completedStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "provisioning_status", nullable = false)
    private TaskRunProvisioningStatus provisioningStatus;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "taskRun")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"taskRun"}, allowSetters = true)
    @Builder.Default
    private Set<TaskRunParam> params = new HashSet<>();

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @NotNull
    @JsonIgnoreProperties(value = { "user", "organization", "cluster", "applications", "accessLists", "jobs", "taskRuns" }, allowSetters = true)
    private Project project;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaskRun)) {
            return false;
        }
        return getId() != null && getId().equals(((TaskRun) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
