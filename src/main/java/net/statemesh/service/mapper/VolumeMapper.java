package net.statemesh.service.mapper;

import net.statemesh.domain.Volume;
import net.statemesh.service.dto.VolumeDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Volume} and its DTO {@link VolumeDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VolumeMapper extends EntityMapper<VolumeDTO, Volume> {
}
