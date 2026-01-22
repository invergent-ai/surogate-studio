package net.statemesh.service.mapper;

import net.statemesh.domain.NodeReservation;
import net.statemesh.service.dto.NodeReservationDTO;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link NodeReservation} and its DTO {@link NodeReservationDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NodeReservationMapper extends EntityMapper<NodeReservationDTO, NodeReservation> {
    @Mapping(target = "node.history", ignore = true)
    NodeReservationDTO toDto(NodeReservation entity, @Context CycleAvoidingMappingContext cycleAvoidingMappingContext);
}
