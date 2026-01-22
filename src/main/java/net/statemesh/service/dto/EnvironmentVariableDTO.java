package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.EnvironmentVariable} entity.
 */
@Data
@SuppressWarnings("common-java:DuplicatedBlocks")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EnvironmentVariableDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 10)
    private String key;

    @NotNull
    @Size(max = 50)
    private String value;

    @Transient
    private Integer containerIndex;

    @JsonIgnore
    private ContainerDTO container;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnvironmentVariableDTO environmentVariableDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, environmentVariableDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
