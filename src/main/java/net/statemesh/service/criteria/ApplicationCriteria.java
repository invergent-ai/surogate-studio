package net.statemesh.service.criteria;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.statemesh.service.criteria.filter.ApplicationModeFilter;
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
public class ApplicationCriteria implements Serializable, Criteria {
    @Serial
    private static final long serialVersionUID = 1L;

    private StringFilter name;
    private ApplicationModeFilter mode;
    private StringFilter alias;
    private StringFilter projectId;

    public ApplicationCriteria(ApplicationCriteria other) {
        this.name = other.name == null ? null : other.name.copy();
        this.mode = other.mode == null ? null : other.mode.copy();
        this.alias = other.alias == null ? null : other.alias.copy();
        this.projectId = other.projectId == null ? null : other.projectId.copy();
    }

    @Override
    public ApplicationCriteria copy() {
        return new ApplicationCriteria(this);
    }

    public StringFilter name() {
        if (name == null) {
            name = new StringFilter();
        }
        return name;
    }

    public ApplicationModeFilter mode() {
        if (mode == null) {
            mode = new ApplicationModeFilter();
        }
        return mode;
    }

    public StringFilter alias() {
        if (alias == null) {
            alias = new StringFilter();
        }
        return alias;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ApplicationCriteria that = (ApplicationCriteria) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(mode, that.mode) &&
            Objects.equals(alias, that.alias) &&
            Objects.equals(projectId, that.projectId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, mode, alias, projectId);
    }

    @Override
    public String toString() {
        return "ApplicationCriteria{" +
            "name=" + name +
            ", mode=" + mode +
            ", alias=" + alias +
            ", projectId=" + projectId +
            '}';
    }
}
