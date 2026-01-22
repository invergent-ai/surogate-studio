package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.statemesh.domain.enumeration.*;
import net.statemesh.k8s.util.ResourceStatus;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A DTO for the {@link net.statemesh.domain.Application} entity.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ApplicationDTO implements ResourceDTO, Serializable {

    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    private String internalName;
    private Double monthlyAppCosts;
    private Double totalAppCosts;

    @Size(max = 50)
    private String alias;
    private Instant firstTx;
    private Boolean fromTemplate;

    @Size(max = 200)
    private String description;

    private String ingressHostName;

    private String deployedNamespace;

    private String errorMessageKey;

    private String warnMessageKey;

    @NotNull
    private ApplicationType type;

    @NotNull
    private ApplicationMode mode;

    @NotNull
    private ApplicationStatus status;

    @NotNull
    private WorkloadType workloadType;

    @Lob
    private String yamlConfig;

    private String pipelineRunName;

    @NotNull
    @Min(value = 1)
    @Max(value = 16)
    private Integer replicas;

    @NotNull
    private UpdateStrategy updateStrategy;

    @NotNull
    private SchedulingRule schedulingRule;

    private String extraConfig;

    private String serviceAccount;

    private Integer progressDeadline;

    private Boolean hostIpc;

    private Boolean hostPid;

    private String runtimeClass;

    @NotNull
    private ProjectDTO project;

    @Builder.Default
    private Set<ContainerDTO> containers = new HashSet<>();
    @Builder.Default
    private Set<AnnotationDTO> annotations = new HashSet<>();
    @Builder.Default
    private Set<LabelDTO> labels = new HashSet<>();

    // Transient
    private boolean keepVolumes;
    private String message;
    @Builder.Default
    private Map<String, String> extraProperties = new HashMap<>();
    @JsonIgnore
    private ExternalDeployDTO externalDeploy;

    @Override
    public String getStatusValue() {
        return Optional.ofNullable(status).map(Enum::name).orElse(null);
    }

    public Set<ResourceAllocationDTO> resourceAllocations() {
        return getContainers().stream()
            .map(container ->
                ResourceAllocationDTO.builder()
                    .replicas(getReplicas())
                    .cpuLimit(container.getCpuLimit())
                    .memLimit(container.getMemLimit())
                    .build()
            )
            .collect(Collectors.toSet());
    }

    public ApplicationDTO withExternalDeploy(ExternalDeployDTO externalDeploy) {
        this.externalDeploy = externalDeploy;
        return this;
    }

    public List<FirewallEntryDTO> ipAllowEntries() {
        return this.getContainers().stream()
            .map(ContainerDTO::getFirewallEntries)
            .flatMap(Set::stream)
            .filter(entry -> FirewallLevel.INGRESS.equals(entry.getLevel()))
            .filter(entry -> PolicyType.INGRESS.equals(entry.getPolicy()))
            .filter(entry -> RuleType.ALLOW.equals(entry.getRule()))
            .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ApplicationDTO applicationDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, applicationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
