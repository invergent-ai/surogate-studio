package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.FirewallLevel;
import net.statemesh.domain.enumeration.PolicyType;
import net.statemesh.domain.enumeration.RuleType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;

/**
 * A FirewallEntry.
 */
@Entity
@Table(name = "firewall_entry")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"container"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class FirewallEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Size(max = 255)
    @Column(name = "cidr", nullable = false)
    private String cidr;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false)
    private FirewallLevel level;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "policy", nullable = false)
    private PolicyType policy;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "rule", nullable = false)
    private RuleType rule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "application", "envVars", "ports", "volumeMounts", "firewallEntries" }, allowSetters = true)
    private Container container;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "project", "volumeMounts", "firewallEntries" }, allowSetters = true)
    private Database database;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FirewallEntry)) {
            return false;
        }
        return getId() != null && getId().equals(((FirewallEntry) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
