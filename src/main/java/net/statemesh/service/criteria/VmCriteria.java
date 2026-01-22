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
public class VmCriteria implements Serializable, Criteria {
    @Serial
    private static final long serialVersionUID = 1L;

    private StringFilter name;
    private StringFilter projectId;

    public VmCriteria(VmCriteria other) {
        this.name = other.name == null ? null : other.name.copy();
        this.projectId = other.projectId == null ? null : other.projectId.copy();
    }

    @Override
    public VmCriteria copy() {
        return new VmCriteria(this);
    }

    public StringFilter name() {
        if (name == null) {
            name = new StringFilter();
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VmCriteria that = (VmCriteria) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(projectId, that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, projectId);
    }

    @Override
    public String toString() {
        return "VmCriteria{" +
            "name=" + name +
            ", projectId=" + projectId +
            '}';
    }
}
