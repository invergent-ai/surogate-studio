package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A Application (includes old PodConfig)
 */
@Entity
@Table(name = "application")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"annotations", "containers", "project"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Application implements Resource, Serializable {
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

    @Size(max = 50)
    @Column(name = "alias", length = 50)
    private String alias;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

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
    @Column(name = "type", nullable = false)
    private ApplicationType type;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, columnDefinition = "varchar(255) default 'APPLICATION'")
    private ApplicationMode mode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "workload_type", nullable = false)
    private WorkloadType workloadType;

    @Column(name = "yaml_config", columnDefinition = "text")
    private String yamlConfig;

    @Column(name = "pipeline_run_name")
    private String pipelineRunName;

    @Column(name = "monthly_app_costs")
    private Double monthlyAppCosts;

    @Column(name = "total_app_costs")
    private Double totalAppCosts;

    @Column(name = "first_tx")
    private Instant firstTx;

    @Column(name = "from_template")
    private Boolean fromTemplate;

    @Column(name = "free_tier")
    private Boolean freeTier;

    @NotNull
    @Min(value = 1)
    @Max(value = 16)
    @Column(name = "replicas", nullable = false)
    private Integer replicas;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "update_strategy", nullable = false)
    private UpdateStrategy updateStrategy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scheduling_rule", nullable = false)
    private SchedulingRule schedulingRule;

    @Column(name = "extra_config", columnDefinition = "text")
    private String extraConfig;

    @Column(name = "service_account")
    private String serviceAccount;

    @Column(name = "progress_deadline")
    @Min(value = 1)
    private Integer progressDeadline;

    @Column(name = "host_ipc")
    private Boolean hostIpc;

    @Column(name = "host_pid")
    private Boolean hostPid;

    @Column(name = "runtime_class")
    private String runtimeClass;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "application")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "application" }, allowSetters = true)
    @Builder.Default
    private Set<Annotation> annotations = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "application")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "application" }, allowSetters = true)
    @Builder.Default
    private Set<Label> labels = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "application")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "application", "envVars", "ports", "volumes" }, allowSetters = true)
    @Builder.Default
    private Set<Container> containers = new HashSet<>();

    @ManyToOne(optional = false, cascade = CascadeType.MERGE)
    @NotNull
    @JsonIgnoreProperties(value = { "user", "organization", "cluster", "applications", "databases", "virtualMachines", "accessLists", "jobs" }, allowSetters = true)
    private Project project;

    public Application containers(Set<Container> containers) {
        this.containers = containers;
        return this;
    }

    public Application annotations(Set<Annotation> annotations) {
        this.annotations = annotations;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Application)) {
            return false;
        }
        return getId() != null && getId().equals(((Application) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
