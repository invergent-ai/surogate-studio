package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import net.statemesh.domain.enumeration.FirewallLevel;
import net.statemesh.domain.enumeration.PolicyType;
import net.statemesh.domain.enumeration.RuleType;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.FirewallEntry} entity.
 */
@Data
@SuppressWarnings("common-java:DuplicatedBlocks")
@ToString(exclude = {"container", "database"})
public class FirewallEntryDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 255)
    private String cidr;

    private FirewallLevel level;
    private PolicyType policy;
    private RuleType rule;

    @JsonIgnore
    private ContainerDTO container;

    @JsonIgnore
    private DatabaseDTO database;


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FirewallEntryDTO firewallEntryDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, firewallEntryDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
