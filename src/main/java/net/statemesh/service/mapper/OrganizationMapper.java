package net.statemesh.service.mapper;

import net.statemesh.domain.Organization;
import net.statemesh.service.dto.OrganizationDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Organization} and its DTO {@link OrganizationDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationMapper extends EntityMapper<OrganizationDTO, Organization> {}
