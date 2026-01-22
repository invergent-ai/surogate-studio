package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Lob;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A DTO for the {@link net.statemesh.domain.Node} entity.
 */
@Data
@SuppressWarnings("common-java:DuplicatedBlocks")
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"resource", "condition", "cluster", "user", "history", "yaml"})
public class NodeDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 50)
    private String name;

    @Size(max = 200)
    private String description;

    @NotNull
    @Size(max = 42)
    private String towerId;


    @NotNull
    private NodeType type;
    private ComputeType computeType;
    private Double hourlyPrice;
    private Double estimatedNodeCosts;
    private Double monthlyNodeEarnings;
    private Instant firstTx;
    private Double totalNodeEarnings;
    private Double ingressPrice;
    private Double freeIngressBytes;
    private Double egressPrice;
    private Double freeEgressBytes;
    private NodeStatus status;
    private Instant lastStartTime;
    private Boolean publicCloud;
    private CloudType cloudType;
    private CloudStatus cloudStatus;
    private NodeRole nodeRole;
    private Double monthlyNodeProfit;
    private Double totalNodeProfit;

    @NotNull
    private Instant creationTime;
    private Instant lastUpdated;
    private Boolean deleted;

    @NotNull
    @Size(max = 100)
    private String internalName;

    @Size(max = 50)
    private String ipv4;

    @Size(max = 50)
    private String ipv6;

    @Size(max = 100)
    private String hostname;

    @Size(max = 10)
    private String architecture;

    @Size(max = 20)
    private String kernelVersion;

    @Size(max = 10)
    private String os;

    @Size(max = 50)
    private String osImage;

    @Size(max = 50)
    private String datacenterName;

    @Size(max = 50)
    private String rayCluster;

    @Size(max = 50)
    private String kubeletVersion;

    private Boolean zoneMatch;

    @Lob
    private String yaml;

    private String credits;

    private NodeResourceDTO resource;
    private NodeConditionDTO condition;
    private ClusterDTO cluster;
    private UserDTO user;

    @JsonIgnore
    private Set<NodeHistoryDTO> history;

    private boolean ready;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeDTO nodeDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, nodeDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    public NodeDTO type(NodeType type) {
        this.type = type;
        return this;
    }

    public NodeDTO resource(NodeResourceDTO resource) {
        this.resource = resource;
        return this;
    }

    public NodeDTO condition(NodeConditionDTO condition) {
        this.condition = condition;
        return this;
    }

    public NodeDTO addHistory(NodeHistoryDTO nodeHistory) {
        if (this.history == null) {
            this.history = new HashSet<>();
        }
        this.history.add(nodeHistory);
        return this;
    }

    public NodeDTO history(Set<NodeHistoryDTO> history) {
        this.setHistory(history);
        return this;
    }

    public NodeDTO hourlyPrice(Double hourlyPrice) {
        this.setHourlyPrice(hourlyPrice);
        return this;
    }

    public NodeDTO delete() {
        this.deleted = Boolean.TRUE;
        return this;
    }
}
