package net.statemesh.service.mapper;

import net.statemesh.domain.Project;
import net.statemesh.service.dto.ProjectDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Project} and its DTO {@link ProjectDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProjectMapper extends EntityMapper<ProjectDTO, Project> {
}
