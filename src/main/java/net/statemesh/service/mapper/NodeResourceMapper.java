package net.statemesh.service.mapper;

import net.statemesh.domain.NodeResource;
import net.statemesh.service.dto.NodeResourceDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link net.statemesh.domain.NodeResource} and its DTO {@link net.statemesh.service.dto.NodeResourceDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NodeResourceMapper extends EntityMapper<NodeResourceDTO, NodeResource> {
}
