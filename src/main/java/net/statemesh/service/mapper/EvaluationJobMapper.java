package net.statemesh.service.mapper;

import net.statemesh.domain.EvaluationJob;
import net.statemesh.service.dto.EvaluationJobDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = {EvaluationBenchmarkMapper.class})
public interface EvaluationJobMapper extends EntityMapper<EvaluationJobDTO, EvaluationJob> {

    @Override
    @Mapping(target = "benchmarks", ignore = true)
    void partialUpdate(@MappingTarget EvaluationJob entity, EvaluationJobDTO dto);
}
