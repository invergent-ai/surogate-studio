package net.statemesh.service.mapper;

import net.statemesh.domain.Database;
import net.statemesh.service.dto.DatabaseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Database} and its DTO {@link DatabaseDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DatabaseMapper extends EntityMapper<DatabaseDTO, Database> {
}
