package net.statemesh.service.mapper;

import net.statemesh.domain.NodeBenchmark;
import net.statemesh.service.dto.NodeBenchmarkDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NodeBenchmarkMapper extends EntityMapper<NodeBenchmarkDTO, NodeBenchmark> {
}
