package net.statemesh.service.mapper;

import net.statemesh.domain.Zone;
import net.statemesh.service.dto.ZoneDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Zone} and its DTO {@link ZoneDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ZoneMapper extends EntityMapper<ZoneDTO, Zone> {
}
