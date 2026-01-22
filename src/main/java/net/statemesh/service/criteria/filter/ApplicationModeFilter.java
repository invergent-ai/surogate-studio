package net.statemesh.service.criteria.filter;

import net.statemesh.domain.enumeration.ApplicationMode;
import tech.jhipster.service.filter.Filter;

import java.io.Serial;

public class ApplicationModeFilter extends Filter<ApplicationMode> {
    @Serial
    private static final long serialVersionUID = 1L;

    public ApplicationModeFilter() {
    }

    public ApplicationModeFilter(ApplicationModeFilter filter) {
        super(filter);
    }

    @Override
    public ApplicationModeFilter copy() {
        return new ApplicationModeFilter(this);
    }
}
