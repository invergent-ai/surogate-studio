package net.statemesh.service.criteria.filter;

import net.statemesh.domain.enumeration.RayJobType;
import tech.jhipster.service.filter.Filter;

import java.io.Serial;

public class RayJobTypeFilter extends Filter<RayJobType> {
    @Serial
    private static final long serialVersionUID = 1L;

    public RayJobTypeFilter() {
    }

    public RayJobTypeFilter(RayJobTypeFilter filter) {
        super(filter);
    }

    @Override
    public RayJobTypeFilter copy() {
        return new RayJobTypeFilter(this);
    }
}
