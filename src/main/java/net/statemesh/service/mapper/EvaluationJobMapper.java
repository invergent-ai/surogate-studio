package net.statemesh.service.mapper;

import net.statemesh.domain.EvaluationJob;
import net.statemesh.service.dto.EvaluationJobDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = { EvaluationBenchmarkMapper.class })
public interface EvaluationJobMapper extends EntityMapper<EvaluationJobDTO, EvaluationJob> {}
