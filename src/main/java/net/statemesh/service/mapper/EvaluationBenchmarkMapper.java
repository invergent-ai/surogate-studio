package net.statemesh.service.mapper;

import net.statemesh.domain.EvaluationBenchmark;
import net.statemesh.service.dto.EvaluationBenchmarkDTO;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface EvaluationBenchmarkMapper extends EntityMapper<EvaluationBenchmarkDTO, EvaluationBenchmark> {

    @Override
    @Mapping(target = "evaluationJob", ignore = true)
    EvaluationBenchmark toEntity(EvaluationBenchmarkDTO dto, @Context CycleAvoidingMappingContext context);

    @Override
    @Mapping(target = "evaluationJob", ignore = true)
    void partialUpdate(@MappingTarget EvaluationBenchmark entity, EvaluationBenchmarkDTO dto);
}
