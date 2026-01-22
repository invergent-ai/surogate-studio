package net.statemesh.service.mapper;

import net.statemesh.domain.NodeHistory;
import net.statemesh.service.dto.NodeHistoryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link NodeHistory} and its DTO {@link NodeHistoryDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NodeHistoryMapper extends EntityMapper<NodeHistoryDTO, NodeHistory> {
}
