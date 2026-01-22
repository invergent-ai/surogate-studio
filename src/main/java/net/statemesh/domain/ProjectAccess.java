package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.statemesh.domain.enumeration.ProjectAccessRole;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;

/**
 * A ProjectAccess.
 */
@Entity
@Table(name = "project_access")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "project"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProjectAccess implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private ProjectAccessRole role;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = {"projects", "nodes", "accessLists", "notifications", "organizations", "sshKeys"},
        allowSetters = true
    )
    private User user;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = {"user", "organization", "cluster", "applications", "accessLists"}, allowSetters = true)
    private Project project;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectAccess)) {
            return false;
        }
        return getId() != null && getId().equals(((ProjectAccess) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
