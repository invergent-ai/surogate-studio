package net.statemesh.service.criteria.filter;

import net.statemesh.domain.enumeration.RayJobProvisioningStatus;
import tech.jhipster.service.filter.Filter;

import java.io.Serial;

public class RayJobProvisioningStatusFilter extends Filter<RayJobProvisioningStatus> {
    @Serial
    private static final long serialVersionUID = 1L;

    public RayJobProvisioningStatusFilter() {
    }

    public RayJobProvisioningStatusFilter(RayJobProvisioningStatusFilter filter) {
        super(filter);
    }

    @Override
    public RayJobProvisioningStatusFilter copy() {
        return new RayJobProvisioningStatusFilter(this);
    }
}
