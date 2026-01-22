package net.statemesh.service.mapper;

import net.statemesh.domain.EnvironmentVariable;
import net.statemesh.service.dto.EnvironmentVariableDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link EnvironmentVariable} and its DTO {@link EnvironmentVariableDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EnvironmentVariableMapper extends EntityMapper<EnvironmentVariableDTO, EnvironmentVariable> {
}
