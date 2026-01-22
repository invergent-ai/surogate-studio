package net.statemesh.service.criteria.filter;

import net.statemesh.domain.enumeration.VolumeType;
import tech.jhipster.service.filter.Filter;

import java.io.Serial;

public class VolumeTypeFilter extends Filter<VolumeType> {
    @Serial
    private static final long serialVersionUID = 1L;

    public VolumeTypeFilter() {
    }

    public VolumeTypeFilter(VolumeTypeFilter filter) {
        super(filter);
    }

    @Override
    public Filter<VolumeType> copy() {
        return new VolumeTypeFilter(this);
    }
}
