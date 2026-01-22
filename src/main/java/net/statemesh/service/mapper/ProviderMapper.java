package net.statemesh.service.mapper;

import net.statemesh.domain.Provider;
import net.statemesh.service.dto.ProviderDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProviderMapper extends EntityMapper<ProviderDTO, Provider> {
}
