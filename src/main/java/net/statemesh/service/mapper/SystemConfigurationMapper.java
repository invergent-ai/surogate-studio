package net.statemesh.service.mapper;

import net.statemesh.domain.SystemConfiguration;
import net.statemesh.service.dto.SystemConfigurationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link SystemConfiguration} and its DTO {@link SystemConfigurationDTO}.
 */
@Mapper(componentModel = "spring")
public interface SystemConfigurationMapper extends EntityMapper<SystemConfigurationDTO, SystemConfiguration> {}
