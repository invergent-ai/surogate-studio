package net.statemesh.service.mapper;

import net.statemesh.domain.RayJob;
import net.statemesh.service.dto.RayJobDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RayJobMapper extends EntityMapper<RayJobDTO, RayJob> {
}
