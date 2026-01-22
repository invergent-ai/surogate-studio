package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.statemesh.domain.enumeration.NodeActionType;
import net.statemesh.domain.enumeration.NodeResourceType;
import net.statemesh.domain.enumeration.NodeUpdateType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * A NodeHistory.
 */
@Entity
@Table(name = "node_history")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"node"})
public class NodeHistory implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Column(name = "time_stamp", nullable = false)
    private Instant timeStamp;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private NodeActionType action;

    @Enumerated(EnumType.STRING)
    @Column(name = "update_type")
    private NodeUpdateType updateType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type")
    private NodeResourceType resourceType;

    @Column(name = "new_yaml", columnDefinition = "text")
    private String newYaml;

    @ManyToOne
    @JsonIgnoreProperties(value = {"history", "user", "units"}, allowSetters = true)
    @JoinColumn(name = "node_id")
    private Node node;
}
