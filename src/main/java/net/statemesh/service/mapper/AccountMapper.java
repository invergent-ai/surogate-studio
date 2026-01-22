package net.statemesh.service.mapper;

import net.statemesh.domain.Project;
import net.statemesh.domain.User;
import net.statemesh.service.dto.ProjectDTO;
import net.statemesh.service.dto.UserDTO;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Project} and its DTO {@link ProjectDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper extends EntityMapper<UserDTO, User> {
    @Mapping(ignore = true, target = "projects")
    @Override
    UserDTO toDto(User s, @Context CycleAvoidingMappingContext context);
}
