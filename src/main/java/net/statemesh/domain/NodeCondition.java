package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;

/**
 * A NodeCondition.
 */
@Entity
@Table(name = "node_condition")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "node" })
public class NodeCondition implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "memory_pressure")
    private Boolean memoryPressure;

    @Column(name = "disk_pressure")
    private Boolean diskPressure;

    @Column(name = "pid_pressure")
    private Boolean pidPressure;

    @Column(name = "kubelet_not_ready")
    private Boolean kubeletNotReady;

    @JsonIgnoreProperties(value = { "condition" }, allowSetters = true)
    @OneToOne(mappedBy = "condition")
    private Node node;

}
