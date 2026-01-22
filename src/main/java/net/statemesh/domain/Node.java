package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import net.statemesh.domain.enumeration.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A Node.
 */
@Entity
@Table(name = "node")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = {"resource", "condition", "cluster", "user", "history", "yaml"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Node implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;


    @Size(max = 42)
    @Column(name = "tower_id", length = 42)
    private String towerId;


    @NotNull
    @Size(max = 50)
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NodeType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "compute_type")
    private ComputeType computeType;

    @Column(name = "hourly_price")
    private Double hourlyPrice;

    @Column(name = "ingress_price")
    private Double ingressPrice;

    @Column(name = "estimated_node_costs")
    private Double estimatedNodeCosts;

    @Column(name = "total_node_earnings")
    private Double totalNodeEarnings;

    @Column(name = "monthly_node_earnings")
    private Double monthlyNodeEarnings;

    @Column(name = "monthly_node_profit")
    private Double monthlyNodeProfit;

    @Column(name = "total_node_profit")
    private Double totalNodeProfit;

    @Column(name = "first_tx")
    private Instant firstTx;

    @Column(name = "egress_price")
    private Double egressPrice;

    @Column(name = "free_ingress_bytes")
    private Long freeIngressBytes;

    @Column(name = "free_egress_bytes")
    private Long freeEgressBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private NodeStatus status;

    @Column(name = "last_start_time")
    private Instant lastStartTime;

    @Column(name = "public_cloud")
    private Boolean publicCloud;

    @Enumerated(EnumType.STRING)
    @Column(name = "cloud_type")
    private CloudType cloudType;

    @Enumerated(EnumType.STRING)
    @Column(name = "cloud_status")
    private CloudStatus cloudStatus;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "node_role", nullable = false)
    private NodeRole nodeRole;

    @NotNull
    @Column(name = "creation_time", nullable = false)
    private Instant creationTime;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "deleted")
    private Boolean deleted;

    @NotNull
    @Size(max = 100)
    @Column(name = "internal_name", length = 100, nullable = false, unique = true)
    private String internalName;

    @Size(max = 50)
    @Column(name = "ipv4", length = 50)
    private String ipv4;

    @Size(max = 50)
    @Column(name = "ipv6", length = 50)
    private String ipv6;

    @Size(max = 100)
    @Column(name = "hostname", length = 100)
    private String hostname;

    @Size(max = 10)
    @Column(name = "architecture", length = 10)
    private String architecture;

    @Size(max = 20)
    @Column(name = "kernel_version", length = 20)
    private String kernelVersion;

    @Size(max = 10)
    @Column(name = "os", length = 10)
    private String os;

    @Size(max = 50)
    @Column(name = "os_image", length = 50)
    private String osImage;

    @Size(max = 50)
    @Column(name = "datacenter_name", length = 50)
    private String datacenterName;

    @Size(max = 50)
    @Column(name = "ray_cluster", length = 50)
    private String rayCluster;

    @Size(max = 50)
    @Column(name = "kubelet_version", length = 50)
    private String kubeletVersion;

    @Column(name = "zone_match")
    private Boolean zoneMatch;

    @Column(name = "yaml", columnDefinition = "text")
    private String yaml;

    @JsonIgnoreProperties(value = {"node"}, allowSetters = true)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(unique = true, name = "node_resource_id")
    private NodeResource resource;

    @JsonIgnoreProperties(value = {"node"}, allowSetters = true)
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(unique = true, name = "node_condition_id")
    private NodeCondition condition;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = {"zone", "nodes", "projects"}, allowSetters = true)
    private Cluster cluster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(
        value = {"projects", "nodes", "accessLists", "notifications", "organizations", "sshKeys"},
        allowSetters = true
    )
    private User user;

    @OneToMany(mappedBy = "node", cascade = CascadeType.ALL, orphanRemoval = true)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"node"}, allowSetters = true)
    @Builder.Default
    private Set<NodeHistory> history = new HashSet<>();

    public Node delete() {
        this.deleted = Boolean.TRUE;
        return this;
    }

    public Node condition(NodeCondition condition) {
        condition.setNode(this);
        this.setCondition(condition);
        return this;
    }

    public Node resource(NodeResource resource) {
        resource.setNode(this);
        this.setResource(resource);
        return this;
    }

    public Node history(Set<NodeHistory> history) {
        history.forEach(hist -> hist.setNode(this));
        this.setHistory(history);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Node)) {
            return false;
        }
        return getId() != null && getId().equals(((Node) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
