package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A Zone.
 */
@Entity
@Table(name = "zone")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"organization", "clusters"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Zone implements Serializable {
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

    @NotNull
    @Column(name = "zone_id", nullable = false, unique = true)
    private String zoneId;

    @Column(name = "vpn_api_key", nullable = false)
    @JsonIgnore
    private String vpnApiKey;

    @Column(name = "iperf_ip", nullable = false)
    @JsonIgnore
    private String iperfIp;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = {"projects", "zones", "users"}, allowSetters = true)
    private Organization organization;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "zone")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = {"zone", "nodes", "projects"}, allowSetters = true)
    @Builder.Default
    private Set<Cluster> clusters = new HashSet<>();

    @Transient
    private Boolean hasHPC;
    @Transient
    private Boolean hasGPU;

    public void setClusters(Set<Cluster> clusters) {
        if (this.clusters != null) {
            this.clusters.forEach(i -> i.setZone(null));
        }
        if (clusters != null) {
            clusters.forEach(i -> i.setZone(this));
        }
        this.clusters = clusters;
    }

    public Zone addClusters(Cluster cluster) {
        this.clusters.add(cluster);
        cluster.setZone(this);
        return this;
    }

    public Zone removeClusters(Cluster cluster) {
        this.clusters.remove(cluster);
        cluster.setZone(null);
        return this;
    }

    public Zone hasHPC(Boolean hasHPC) {
        this.hasHPC = hasHPC;
        return this;
    }

    public Zone hasGPU(Boolean hasGPU) {
        this.hasGPU = hasGPU;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Zone)) {
            return false;
        }
        return getId() != null && getId().equals(((Zone) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
