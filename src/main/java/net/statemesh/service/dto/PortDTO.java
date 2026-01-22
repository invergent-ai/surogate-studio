package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.Port} entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"container"})
@SuppressWarnings("common-java:DuplicatedBlocks")
public class PortDTO implements Serializable {

    private String id;

    @NotNull
    private String name;

    @NotNull
    private Integer containerPort;

    @NotNull
    private Boolean ingressPort;

    private String ingressHost;

    private Integer servicePort;

    private ProtocolDTO protocol;

    @JsonIgnore
    private ContainerDTO container;

    @Transient
    private Integer containerIndex;
    @Transient
    private Integer targetPort;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PortDTO portDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, portDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
