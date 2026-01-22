package net.statemesh.service.criteria;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.Filter;
import tech.jhipster.service.filter.StringFilter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Criteria class for the {@link net.statemesh.domain.Node} entity. This class is used
 * in {@link net.statemesh.web.rest.NodeResource} to receive all the possible filtering options from
 * the Http GET request parameters.
 * For example the following could be a valid request:
 * {@code /node?id.greaterThan=5&attr1.contains=something&attr2.specified=false}
 * As Spring is unable to properly convert the types, unless specific {@link Filter} class are used, we need to use
 * fix type specific filters.
 */
@Setter
@Getter
@ParameterObject
@SuppressWarnings("common-java:DuplicatedBlocks")
@NoArgsConstructor
public class NodeCriteria implements Serializable, Criteria {
    @Serial
    private static final long serialVersionUID = 1L;

    private StringFilter name;
    private StringFilter description;
    private StringFilter datacenterName;

    public NodeCriteria(NodeCriteria other) {
        this.name = other.name == null ? null : other.name.copy();
        this.description = other.description == null ? null : other.description.copy();
        this.datacenterName = other.datacenterName == null ? null : other.datacenterName.copy();
    }

    @Override
    public NodeCriteria copy() {
        return new NodeCriteria(this);
    }

    public StringFilter name() {
        if (name == null) {
            name = new StringFilter();
        }
        return name;
    }

    public StringFilter description() {
        if (description == null) {
            description = new StringFilter();
        }
        return description;
    }

    public StringFilter datacenterName() {
        if (datacenterName == null) {
            datacenterName = new StringFilter();
        }
        return datacenterName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final NodeCriteria that = (NodeCriteria) o;
        return (
            Objects.equals(name, that.name) &&
            Objects.equals(description, that.description) &&
            Objects.equals(datacenterName, that.datacenterName)
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            name,
            description,
            datacenterName
        );
    }

    @Override
    public String toString() {
        return "NodeCriteria{" +
            "name=" + name +
            ", description=" + description +
            ", datacenterName=" + datacenterName +
            '}';
    }
}
