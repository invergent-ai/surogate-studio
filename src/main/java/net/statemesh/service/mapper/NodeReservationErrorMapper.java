package net.statemesh.service.mapper;

import net.statemesh.domain.NodeReservationError;
import net.statemesh.service.dto.NodeReservationErrorDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NodeReservationErrorMapper extends EntityMapper<NodeReservationErrorDTO, NodeReservationError> {
}
