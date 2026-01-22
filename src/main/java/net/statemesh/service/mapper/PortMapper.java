package net.statemesh.service.mapper;

import net.statemesh.domain.Port;
import net.statemesh.service.dto.PortDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Port} and its DTO {@link PortDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PortMapper extends EntityMapper<PortDTO, Port> {
}
