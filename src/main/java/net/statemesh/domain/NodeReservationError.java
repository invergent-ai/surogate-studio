package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "node_reservation_error")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"nodeReservation"})
public class NodeReservationError implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Column(name = "created", nullable = false)
    private Instant created;

    @Column(name = "error", columnDefinition = "text")
    private String error;

    @Column(name = "errors", columnDefinition = "text")
    private String errors;

    @ManyToOne
    @JsonIgnoreProperties(value = { "user", "node" }, allowSetters = true)
    private NodeReservation nodeReservation;
}

