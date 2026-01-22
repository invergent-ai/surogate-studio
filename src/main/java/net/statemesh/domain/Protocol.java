package net.statemesh.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.io.Serial;
import java.io.Serializable;

import lombok.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Protocol LOV
 */
@Entity
@Table(name = "protocol")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Protocol implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @NotNull
    @Size(max = 10)
    @Column(name = "p_code", length = 10, nullable = false)
    private String code;

    @NotNull
    @Size(max = 50)
    @Column(name = "p_value", length = 50, nullable = false)
    private String value;

    @Column(name = "port")
    private Integer port;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Protocol)) {
            return false;
        }
        return getId() != null && getId().equals(((Protocol) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }
}
