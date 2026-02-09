package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Cluster.
 */
@Entity
@Table(name = "cluster")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"zone", "nodes", "projects"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Cluster implements Serializable {

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

    @NotNull
    @Column(name = "cid", nullable = false, unique = true)
    private String cid;

    @Column(name = "kube_config", columnDefinition = "text")
    private String kubeConfig;

    @Column(name = "prometheus_url")
    private String prometheusUrl;

    @Column(name = "redis_url")
    private String redisUrl;

    @Column(name = "request_vs_limits_coefficient_cpu")
    private Double requestVsLimitsCoefficientCpu;

    @Column(name = "request_vs_limits_coefficient_memory")
    private Double requestVsLimitsCoefficientMemory;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "organization", "clusters" }, allowSetters = true)
    private Zone zone;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "cluster")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "cluster", "user", "units" }, allowSetters = true)
    @Builder.Default
    private Set<Node> nodes = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "cluster")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "user", "organization", "cluster", "applications", "accessLists" }, allowSetters = true)
    @Builder.Default
    private Set<Project> projects = new HashSet<>();

    public void setNodes(Set<Node> nodes) {
        if (this.nodes != null) {
            this.nodes.forEach(i -> i.setCluster(null));
        }
        if (nodes != null) {
            nodes.forEach(i -> i.setCluster(this));
        }
        this.nodes = nodes;
    }

    public void setProjects(Set<Project> projects) {
        if (this.projects != null) {
            this.projects.forEach(i -> i.setCluster(null));
        }
        if (projects != null) {
            projects.forEach(i -> i.setCluster(this));
        }
        this.projects = projects;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cluster)) {
            return false;
        }
        return getId() != null && getId().equals(((Cluster) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
