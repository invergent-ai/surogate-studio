package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.Profile;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Project.
 */
@Entity
@Table(name = "project")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "organization", "cluster", "applications", "accessLists"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Project implements Serializable {

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

    @Size(max = 50)
    @Column(name = "alias", length = 50)
    private String alias;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "namespace", unique = true)
    private String namespace;

    @Column(name = "datacenter_name")
    private String datacenterName;

    @Column(name = "ray_cluster")
    private String rayCluster;

    @Enumerated(EnumType.STRING)
    @Column(name = "profile")
    private Profile profile;

    @Column(name = "deleted")
    private Boolean deleted;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = {"projects", "nodes", "accessLists", "notifications", "organizations", "sshKeys"},
        allowSetters = true
    )
    private User user;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = {"projects", "zones", "users"}, allowSetters = true)
    private Organization organization;

    // we first assign the zone to a project when it's created
    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "organization", "clusters" }, allowSetters = true)
    private Zone zone;

    // the cluster is set on the first deployed resource, based on a cluster selection strategy
    @ManyToOne(fetch = FetchType.EAGER)
    @JsonIgnoreProperties(value = {"zone", "nodes", "projects"}, allowSetters = true)
    private Cluster cluster;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
    @JsonIgnoreProperties(value = {"application", "project"}, allowSetters = true)
    @Builder.Default
    private Set<Application> applications = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
    @JsonIgnoreProperties(value = {"project"}, allowSetters = true)
    @Builder.Default
    private Set<Database> databases = new HashSet<>();


    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
    @JsonIgnoreProperties(value = {"project"}, allowSetters = true)
    @Builder.Default
    private Set<RayJob> jobs = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
    @JsonIgnoreProperties(value = {"project"}, allowSetters = true)
    @Builder.Default
    private Set<TaskRun> taskRuns = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
    @JsonIgnoreProperties(value = {"project"}, allowSetters = true)
    @Builder.Default
    private Set<Volume> volumes = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "project")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"user", "project"}, allowSetters = true)
    @Builder.Default
    private Set<ProjectAccess> accessLists = new HashSet<>();

    public void delete() {
        this.setDeleted(Boolean.TRUE);
    }

    public void setApplications(Set<Application> applications) {
        if (this.applications != null) {
            this.applications.forEach(i -> i.setProject(null));
        }
        if (applications != null) {
            applications.forEach(i -> i.setProject(this));
        }
        this.applications = applications;
    }

    public void setAccessLists(Set<ProjectAccess> projectAccesses) {
        if (this.accessLists != null) {
            this.accessLists.forEach(i -> i.setProject(null));
        }
        if (projectAccesses != null) {
            projectAccesses.forEach(i -> i.setProject(this));
        }
        this.accessLists = projectAccesses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Project)) {
            return false;
        }
        return getId() != null && getId().equals(((Project) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
