package net.statemesh.service.criteria;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.statemesh.service.criteria.filter.TaskRunProvisioningStatusFilter;
import net.statemesh.service.criteria.filter.TaskRunTypeFilter;
import org.springdoc.core.annotations.ParameterObject;
import tech.jhipster.service.Criteria;
import tech.jhipster.service.filter.InstantFilter;
import tech.jhipster.service.filter.StringFilter;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@ParameterObject
@NoArgsConstructor
public class TaskRunCriteria implements Serializable, Criteria {
    @Serial
    private static final long serialVersionUID = 1L;

    private StringFilter name;
    private StringFilter projectId;
    private TaskRunTypeFilter type;
    private TaskRunProvisioningStatusFilter provisioningStatus;
    private InstantFilter startTime;

    public TaskRunCriteria(TaskRunCriteria other) {
        this.name = other.name == null ? null : other.name.copy();
        this.projectId = other.projectId == null ? null : other.projectId.copy();
        this.type = other.type == null ? null : other.type.copy();
        this.provisioningStatus = other.provisioningStatus == null ? null : other.provisioningStatus.copy();
        this.startTime = other.startTime == null ? null : other.startTime.copy();
    }

    @Override
    public TaskRunCriteria copy() {
        return new TaskRunCriteria(this);
    }

    public StringFilter name() {
        if (name == null) {
            name = new StringFilter();
        }
        return name;
    }

    public TaskRunTypeFilter type() {
        if (type == null) {
            type = new TaskRunTypeFilter();
        }
        return type;
    }

    public TaskRunProvisioningStatusFilter provisioningStatus() {
        if (provisioningStatus == null) {
            provisioningStatus = new TaskRunProvisioningStatusFilter();
        }
        return provisioningStatus;
    }

    public InstantFilter startTime() {
        if (startTime == null) {
            startTime = new InstantFilter();
        }
        return startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskRunCriteria that = (TaskRunCriteria) o;
        return Objects.equals(name, that.name) &&
            Objects.equals(projectId, that.projectId) &&
            Objects.equals(type, that.type) &&
            Objects.equals(provisioningStatus, that.provisioningStatus) &&
            Objects.equals(startTime, that.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, projectId, type, provisioningStatus, startTime);
    }

    @Override
    public String toString() {
        return "TaskRunCriteria{" +
            "name=" + name +
            ", projectId=" + projectId +
            ", type=" + type +
            ", provisioningStatus=" + provisioningStatus +
            ", startTime=" + startTime +
            '}';
    }
}
