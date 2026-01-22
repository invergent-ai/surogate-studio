package net.statemesh.service.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.Protocol} entity.
 */
@Data
@Schema(description = "Protocol LOV")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProtocolDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 10)
    private String code;

    @NotNull
    @Size(max = 50)
    private String value;

    private Integer port;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProtocolDTO)) {
            return false;
        }

        ProtocolDTO protocolDTO = (ProtocolDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, protocolDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
