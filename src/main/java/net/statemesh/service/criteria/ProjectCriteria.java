package net.statemesh.service.criteria;

import lombok.Getter;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.Filter;
import tech.jhipster.service.filter.StringFilter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the {@link net.statemesh.domain.Project} entity. This class is used
 * in {@link net.statemesh.web.rest.ProjectResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /project?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@ParameterObject
@Getter
@Setter
@SuppressWarnings("common-java:DuplicatedBlocks")
public class ProjectCriteria implements Serializable, Criteria {
    @Serial
    private static final long serialVersionUID = 1L;

    private StringFilter name;
    private StringFilter alias;
    private StringFilter description;
    private StringFilter zoneId;

    public ProjectCriteria() {}

    public ProjectCriteria(ProjectCriteria other) {
        this.name = other.name == null ? null : other.name.copy();
        this.alias = other.alias == null ? null : other.alias.copy();
        this.description = other.description == null ? null : other.description.copy();
        this.zoneId = other.zoneId == null ? null : other.zoneId.copy();
    }

    @Override
    public ProjectCriteria copy() {
        return new ProjectCriteria(this);
    }

    public StringFilter name() {
        if (name == null) {
            name = new StringFilter();
        }
        return name;
    }

    public StringFilter alias() {
        if (alias == null) {
            alias = new StringFilter();
        }
        return alias;
    }

    public StringFilter description() {
        if (description == null) {
            description = new StringFilter();
        }
        return description;
    }

    public StringFilter zoneId() {
        if (zoneId == null) {
            zoneId = new StringFilter();
        }
        return zoneId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final ProjectCriteria that = (ProjectCriteria) o;
        return (
            Objects.equals(name, that.name) &&
            Objects.equals(alias, that.alias) &&
            Objects.equals(description, that.description) &&
            Objects.equals(zoneId, that.zoneId)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            alias,
            description,
            zoneId
        );
    }

    @Override
    public String toString() {
        return "ProjectCriteria{" +
            "name=" + name +
            ", alias=" + alias +
            ", description=" + description +
            ", zoneId=" + zoneId +
            '}';
    }
}
