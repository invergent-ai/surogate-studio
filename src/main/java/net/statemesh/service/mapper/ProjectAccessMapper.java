package net.statemesh.service.mapper;

import net.statemesh.domain.ProjectAccess;
import net.statemesh.service.dto.ProjectAccessDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link ProjectAccess} and its DTO {@link ProjectAccessDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjectAccessMapper extends EntityMapper<ProjectAccessDTO, ProjectAccess> {
}
