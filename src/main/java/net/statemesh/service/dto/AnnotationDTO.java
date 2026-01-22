package net.statemesh.service.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * A DTO for the {@link net.statemesh.domain.Annotation} entity.
 */
@Data
@SuppressWarnings("common-java:DuplicatedBlocks")
public class AnnotationDTO implements Serializable {

    private String id;

    @NotNull
    @Size(max = 10)
    private String key;

    @NotNull
    @Size(max = 50)
    private String value;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AnnotationDTO annotationDTO)) {
            return false;
        }

        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, annotationDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
