package net.statemesh.service.mapper;

import net.statemesh.domain.TaskRun;
import net.statemesh.service.dto.TaskRunDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskRunMapper extends EntityMapper<TaskRunDTO, TaskRun> {
}
