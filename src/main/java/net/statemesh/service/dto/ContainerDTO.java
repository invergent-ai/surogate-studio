package net.statemesh.service.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.statemesh.domain.enumeration.ContainerType;
import net.statemesh.domain.enumeration.PullImageMode;
import org.thymeleaf.util.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * A DTO for the {@link net.statemesh.domain.Container} entity.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"volumeMounts", "envVars", "ports", "firewallEntries"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ContainerDTO implements Serializable {

    private String id;

    private String displayName;

    @NotNull
    private String imageName;

    @NotNull
    private String imageTag;

    @NotNull
    private ContainerType type;

    @NotNull
    private PullImageMode pullImageMode;

    private String registryUrl;

    private String registryUsername;

    @Min(value = 0)
    @Max(value = 100)
    // Computed programmatically: coefficient * cpuLimit
    private Double cpuRequest;

    @Min(value = 0)
    @Max(value = 100)
    private Double cpuLimit;

    private String memRequest;

    private String memLimit;

    private String startCommand;

    private String startParameters;

    @Builder.Default
    private Set<EnvironmentVariableDTO> envVars = new HashSet<>();
    @Builder.Default
    private Set<VolumeMountDTO> volumeMounts = new HashSet<>();
    @Builder.Default
    private Set<PortDTO> ports = new HashSet<>();
    @Builder.Default
    private Set<FirewallEntryDTO> firewallEntries = new HashSet<>();
    @Builder.Default
    private Set<ProbeDTO> probes = new HashSet<>();

    @Min(value = 0)
    @Max(value = 100)
    private Integer gpuLimit;

    // Transient
    private String registryPassword;
    private Integer replicas;

    @Builder.Default
    private Map<String, String> extraProperties = new HashMap<>();

    public String memRequest() {
        return StringUtils.concat(this.memRequest, "Mi");
    }

    public String memLimit() {
        return StringUtils.concat(this.memLimit, "Mi");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContainerDTO containerDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, containerDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
