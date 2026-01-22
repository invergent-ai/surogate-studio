package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * A NodeReservation.
 */
@Entity
@Table(name = "node_reservation")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user"})
public class NodeReservation implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Column(name = "created", nullable = false)
    private Instant created;

    @Column(name = "updated")
    private Instant updated;

    @Size(max = 255)
    @Column(name = "access_token")
    private String accessToken;

    @NotNull
    @Size(max = 100)
    @Column(name = "internal_name", nullable = false, length = 100)
    private String internalName;

    @NotNull
    @Column(name = "expire_time", nullable = false)
    private Instant expireTime;

    @NotNull
    @Size(max = 512)
    @Column(name = "sm_id", nullable = false)
    private String smId;

    @NotNull
    @Size(max = 256)
    @Column(name = "short_sm_id", nullable = false)
    private String shortSmId;

    @Size(max = 63)
    @Column(name = "user_key")
    private String userKey;

    @Size(max = 50)
    @Column(name = "zone_id", length = 50)
    private String zoneId;

    @Size(max = 50)
    @Column(name = "machine_id", length = 50)
    private String machineId;

    @Size(max = 50)
    @Column(name = "boot_id", length = 50)
    private String bootId;

    @Size(max = 20)
    @Column(name = "ip", length = 20)
    private String ip;

    @Column(name = "deleted")
    private Boolean deleted;

    @Column(name = "fulfilled")
    private Boolean fulfilled;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(
        value = {"projects", "nodes", "accessLists", "notifications", "organizations", "reservations", "sshKeys"},
        allowSetters = true
    )
    private User user;

    @ManyToOne
    @JsonIgnoreProperties(value = { "cluster", "user", "units", "history", "resource", "condition" }, allowSetters = true)
    private Node node;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "nodeReservation")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "nodeReservation" }, allowSetters = true)
    @Builder.Default
    private Set<NodeBenchmark> benchmarks = new HashSet<>();

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "nodeReservation")
    @Cache(usage = CacheConcurrencyStrategy.NONE)
    @JsonIgnoreProperties(value = { "nodeReservation" }, allowSetters = true)
    @Builder.Default
    private Set<NodeReservationError> errors = new HashSet<>();
}
