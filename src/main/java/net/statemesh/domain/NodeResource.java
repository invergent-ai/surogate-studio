package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A NodeResource.
 */
@Entity
@Table(name = "node_resource")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"node"})
public class NodeResource implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "rx_mbps")
    private Integer rxMbps;

    @Column(name = "tx_mbps")
    private Integer txMbps;

    @Column(name = "allocatable_cpu", precision = 21, scale = 2)
    private BigDecimal allocatableCpu;

    @Column(name = "allocatable_memory", precision = 21, scale = 2)
    private BigDecimal allocatableMemory;

    @Column(name = "allocatable_ephemeral_storage", precision = 21, scale = 2)
    private BigDecimal allocatableEphemeralStorage;

    @Column(name = "capacity_cpu", precision = 21, scale = 2)
    private BigDecimal capacityCpu;

    @Column(name = "capacity_memory", precision = 21, scale = 2)
    private BigDecimal capacityMemory;

    @Column(name = "capacity_ephemeral_storage", precision = 21, scale = 2)
    private BigDecimal capacityEphemeralStorage;

    @JsonIgnoreProperties(value = {"resource"}, allowSetters = true)
    @OneToOne(mappedBy = "resource")
    private Node node;

}
