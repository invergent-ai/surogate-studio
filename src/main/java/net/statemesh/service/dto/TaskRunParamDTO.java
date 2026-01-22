package net.statemesh.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Data
@SuppressWarnings("common-java:DuplicatedBlocks")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"taskRun"})
public class TaskRunParamDTO implements Serializable {

    private String id;

    @NotNull
    private String key;

    @NotNull
    private String value;

    @JsonIgnore
    private TaskRunDTO taskRun;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TaskRunParamDTO dto)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, dto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
