package net.statemesh.service.mapper;

import net.statemesh.domain.Application;
import net.statemesh.service.dto.ApplicationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Application} and its DTO {@link ApplicationDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApplicationMapper extends EntityMapper<ApplicationDTO, Application> {
}
