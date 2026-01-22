package net.statemesh.service.mapper;

import net.statemesh.domain.VolumeMount;
import net.statemesh.service.dto.VolumeMountDTO;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link VolumeMount} and its DTO {@link VolumeMountDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VolumeMountMapper extends EntityMapper<VolumeMountDTO, VolumeMount> {
    @Mapping(source = "container.application.id", target = "applicationId")
    @Mapping(source = "database.id", target = "databaseId")
    VolumeMountDTO toDto(VolumeMount volumeMount, @Context CycleAvoidingMappingContext cycleAvoidingMappingContext);
}
