package net.statemesh.service.mapper;

import net.statemesh.domain.Notification;
import net.statemesh.service.dto.NotificationDTO;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Notification} and its DTO {@link NotificationDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper extends EntityMapper<NotificationDTO, Notification> {
    @Mapping(target = "extraProperties", source = "extraProperties", ignore = true)
    NotificationDTO toDto(Notification s, @Context CycleAvoidingMappingContext context);

    @Mapping(target = "extraProperties", source = "extraProperties", ignore = true)
    Notification toEntity(NotificationDTO s, @Context CycleAvoidingMappingContext context);
}
