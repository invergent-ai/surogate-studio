package net.statemesh.service.mapper;

import net.statemesh.domain.JobEnvironmentVariable;
import net.statemesh.service.dto.JobEnvironmentVariableDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface JobEnvironmentVariableMapper extends EntityMapper<JobEnvironmentVariableDTO, JobEnvironmentVariable> {
}
