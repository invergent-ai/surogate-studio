package net.statemesh.service.mapper;

import net.statemesh.domain.Protocol;
import net.statemesh.service.dto.ProtocolDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Protocol} and its DTO {@link ProtocolDTO}.
 */
@Mapper(componentModel = "spring")
public interface ProtocolMapper extends EntityMapper<ProtocolDTO, Protocol> {}
