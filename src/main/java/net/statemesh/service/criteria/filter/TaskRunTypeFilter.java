package net.statemesh.service.criteria.filter;

import net.statemesh.domain.enumeration.TaskRunType;
import tech.jhipster.service.filter.Filter;

import java.io.Serial;

public class TaskRunTypeFilter extends Filter<TaskRunType> {
    @Serial
    private static final long serialVersionUID = 1L;

    public TaskRunTypeFilter() {
    }

    public TaskRunTypeFilter(TaskRunTypeFilter filter) {
        super(filter);
    }

    @Override
    public TaskRunTypeFilter copy() {
        return new TaskRunTypeFilter(this);
    }
}
