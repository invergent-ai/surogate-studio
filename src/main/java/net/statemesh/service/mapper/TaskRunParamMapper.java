package net.statemesh.service.mapper;

import net.statemesh.domain.TaskRunParam;
import net.statemesh.service.dto.TaskRunParamDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskRunParamMapper extends EntityMapper<TaskRunParamDTO, TaskRunParam> {
}
