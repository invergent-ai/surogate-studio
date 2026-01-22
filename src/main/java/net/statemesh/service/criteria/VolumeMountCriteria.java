package net.statemesh.service.criteria;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.StringFilter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@ParameterObject
@NoArgsConstructor
public class VolumeMountCriteria implements Serializable, Criteria {
    @Serial
    private static final long serialVersionUID = 1L;

    private StringFilter projectId;
    private StringFilter volumeId;

    public VolumeMountCriteria(VolumeMountCriteria other) {
        this.projectId = other.projectId == null ? null : other.projectId.copy();
        this.volumeId = other.volumeId == null ? null : other.volumeId.copy();
    }

    @Override
    public VolumeMountCriteria copy() {
        return new VolumeMountCriteria(this);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VolumeMountCriteria that = (VolumeMountCriteria) o;
        return Objects.equals(projectId, that.projectId) &&
            Objects.equals(volumeId, that.volumeId);
    }

    @Override
    public String toString() {
        return "VolumeCriteria{" +
            ", projectId=" + projectId +
            ", volumeId=" + volumeId +
            '}';
    }
}
