package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import lombok.*;
import net.statemesh.domain.enumeration.OrganizationType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Organization.
 */
@Entity
@Table(name = "organization")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "projects", "zones", "users" })
public class Organization implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Size(max = 50)
    @Column(name = "name", length = 50, nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private OrganizationType type;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "organization")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "user", "organization", "cluster", "applications", "accessLists" }, allowSetters = true)
    @Builder.Default
    private Set<Project> projects = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "organization")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "organization", "clusters" }, allowSetters = true)
    @Builder.Default
    private Set<Zone> zones = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "organization")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "user", "organization" }, allowSetters = true)
    @Builder.Default
    private Set<UserXOrganization> users = new HashSet<>();

    public void setProjects(Set<Project> projects) {
        if (this.projects != null) {
            this.projects.forEach(i -> i.setOrganization(null));
        }
        if (projects != null) {
            projects.forEach(i -> i.setOrganization(this));
        }
        this.projects = projects;
    }

    public Organization addProjects(Project project) {
        this.projects.add(project);
        project.setOrganization(this);
        return this;
    }

    public Organization removeProjects(Project project) {
        this.projects.remove(project);
        project.setOrganization(null);
        return this;
    }

    public void setZones(Set<Zone> zones) {
        if (this.zones != null) {
            this.zones.forEach(i -> i.setOrganization(null));
        }
        if (zones != null) {
            zones.forEach(i -> i.setOrganization(this));
        }
        this.zones = zones;
    }

    public Organization addZones(Zone zone) {
        this.zones.add(zone);
        zone.setOrganization(this);
        return this;
    }

    public Organization removeZones(Zone zone) {
        this.zones.remove(zone);
        zone.setOrganization(null);
        return this;
    }

    public void setUsers(Set<UserXOrganization> userXOrganizations) {
        if (this.users != null) {
            this.users.forEach(i -> i.setOrganization(null));
        }
        if (userXOrganizations != null) {
            userXOrganizations.forEach(i -> i.setOrganization(this));
        }
        this.users = userXOrganizations;
    }

    public Organization addUsers(UserXOrganization userXOrganization) {
        this.users.add(userXOrganization);
        userXOrganization.setOrganization(this);
        return this;
    }

    public Organization removeUsers(UserXOrganization userXOrganization) {
        this.users.remove(userXOrganization);
        userXOrganization.setOrganization(null);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Organization)) {
            return false;
        }
        return getId() != null && getId().equals(((Organization) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
