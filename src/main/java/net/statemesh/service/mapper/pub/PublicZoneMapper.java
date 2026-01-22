package net.statemesh.service.mapper.pub;

import net.statemesh.domain.Zone;
import net.statemesh.service.dto.pub.PublicZoneDTO;
import net.statemesh.service.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PublicZoneMapper extends EntityMapper<PublicZoneDTO, Zone> {
}
