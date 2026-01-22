package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import net.statemesh.domain.enumeration.ProbeType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;

@Entity
@Table(name = "probe")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"container"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Probe implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    ProbeType type;

    @Column(name = "initial_delay")
    Integer initialDelaySeconds;

    @Column(name = "period")
    Integer periodSeconds;

    @Column(name = "failure_threshold")
    Integer failureThreshold;

    @Column(name = "success_threshold")
    Integer successThreshold;

    @Column(name = "timeout")
    Integer timeoutSeconds;

    @Column(name = "termination_grace_period")
    Long terminationGracePeriodSeconds;

    @Column(name = "http_path")
    String httpPath;

    @Column(name = "http_port")
    Integer httpPort;

    @Column(name = "tcp_host")
    String tcpHost;

    @Column(name = "tcp_port")
    Integer tcpPort;

    @Column(name = "exec_command")
    @ElementCollection
    List<String> execCommand;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "application", "envVars", "ports", "volumes" }, allowSetters = true)
    private Container container;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Probe)) {
            return false;
        }
        return getId() != null && getId().equals(((Probe) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
