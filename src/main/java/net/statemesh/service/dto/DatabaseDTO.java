package net.statemesh.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.DatabaseStatus;
import net.statemesh.domain.enumeration.FirewallLevel;
import net.statemesh.domain.enumeration.PolicyType;
import net.statemesh.domain.enumeration.RuleType;
import net.statemesh.k8s.util.ResourceStatus;
import org.thymeleaf.util.StringUtils;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
@ToString(exclude = {"volumeMounts", "firewallEntries"})
public class DatabaseDTO implements ResourceDTO, Serializable {

    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    private String internalName;

    @Size(max = 200)
    private String description;

    private Boolean hasIngress;
    private String ingressHostName;

    private String deployedNamespace;

    private String errorMessageKey;

    private String warnMessageKey;

    @NotNull
    private DatabaseStatus status;

    private Collection<ResourceStatus> resourceStatus;

    private Double cpuLimit;
    private String memLimit;

    @NotNull
    @Min(value = 1)
    @Max(value = 16)
    private Integer replicas;

    @NotNull
    private ProjectDTO project;

    @Builder.Default
    private Set<VolumeMountDTO> volumeMounts = new HashSet<>();
    @Builder.Default
    private Set<FirewallEntryDTO> firewallEntries = new HashSet<>();

    // Transient
    private boolean keepVolumes;
    // Transient
    private String message;

    public String memLimit() {
        return StringUtils.concat(this.memLimit, "Mi");
    }

    public Set<ResourceAllocationDTO> resourceAllocations() {
        return Set.of(
            ResourceAllocationDTO.builder()
                .replicas(getReplicas())
                .cpuLimit(getCpuLimit())
                .memLimit(getMemLimit())
                .build()
        );
    }

    public List<FirewallEntryDTO> ipAllowEntries() {
        return this.getFirewallEntries().stream()
            .filter(entry -> FirewallLevel.INGRESS.equals(entry.getLevel()))
            .filter(entry -> PolicyType.INGRESS.equals(entry.getPolicy()))
            .filter(entry -> RuleType.ALLOW.equals(entry.getRule()))
            .toList();
    }

    @Override
    public String getStatusValue() {
        return Optional.ofNullable(status).map(Enum::name).orElse(null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DatabaseDTO databaseDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, databaseDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
