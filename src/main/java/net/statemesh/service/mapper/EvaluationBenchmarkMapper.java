package net.statemesh.service.mapper;

import net.statemesh.domain.EvaluationBenchmark;
import net.statemesh.service.dto.EvaluationBenchmarkDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EvaluationBenchmarkMapper extends EntityMapper<EvaluationBenchmarkDTO, EvaluationBenchmark> {}

