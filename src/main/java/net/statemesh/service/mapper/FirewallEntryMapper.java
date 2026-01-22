package net.statemesh.service.mapper;

import net.statemesh.domain.FirewallEntry;
import net.statemesh.service.dto.FirewallEntryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link FirewallEntry} and its DTO {@link FirewallEntryDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FirewallEntryMapper extends EntityMapper<FirewallEntryDTO, FirewallEntry> {
}
