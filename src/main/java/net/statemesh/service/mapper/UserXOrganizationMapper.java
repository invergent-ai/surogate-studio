package net.statemesh.service.mapper;

import net.statemesh.domain.UserXOrganization;
import net.statemesh.service.dto.UserXOrganizationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link UserXOrganization} and its DTO {@link UserXOrganizationDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserXOrganizationMapper extends EntityMapper<UserXOrganizationDTO, UserXOrganization> {
}
