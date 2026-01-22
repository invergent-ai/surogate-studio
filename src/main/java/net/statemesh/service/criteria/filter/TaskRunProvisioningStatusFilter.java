package net.statemesh.service.criteria.filter;

import net.statemesh.domain.enumeration.TaskRunProvisioningStatus;
import tech.jhipster.service.filter.Filter;

import java.io.Serial;

public class TaskRunProvisioningStatusFilter extends Filter<TaskRunProvisioningStatus> {
    @Serial
    private static final long serialVersionUID = 1L;

    public TaskRunProvisioningStatusFilter() {
    }

    public TaskRunProvisioningStatusFilter(TaskRunProvisioningStatusFilter filter) {
        super(filter);
    }

    @Override
    public TaskRunProvisioningStatusFilter copy() {
        return new TaskRunProvisioningStatusFilter(this);
    }
}
