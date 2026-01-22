package net.statemesh.service.mapper;

import net.statemesh.domain.NodeCondition;
import net.statemesh.service.dto.NodeConditionDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link NodeCondition} and its DTO {@link NodeConditionDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NodeConditionMapper extends EntityMapper<NodeConditionDTO, NodeCondition> {
}
