package net.statemesh.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serial;
import java.io.Serializable;

/**
 * A Port.
 */
@Entity
@Table(name = "port")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"container"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Port implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "container_port", nullable = false)
    private Integer containerPort;

    @Column(name = "service_port")
    private Integer servicePort;

    @ManyToOne(optional = false)
    @NotNull
    private Protocol protocol;

    @Column(name = "ingress_port")
    private Boolean ingressPort;

    @Column(name = "ingress_host")
    private String ingressHost;

    @ManyToOne
    @JsonIgnoreProperties(value = {"application", "envVars", "ports", "volumes"}, allowSetters = true)
    private Container container;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Port)) {
            return false;
        }
        return getId() != null && getId().equals(((Port) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
