package net.statemesh.service.mapper;

import net.statemesh.domain.AppTemplate;
import net.statemesh.service.dto.AppTemplateDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link AppTemplate} and its DTO {@link AppTemplateDTO}.
 */
@Mapper(componentModel = "spring",
    uses = {ProviderMapper.class},
    unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AppTemplateMapper extends EntityMapper<AppTemplateDTO, AppTemplate> {
}
