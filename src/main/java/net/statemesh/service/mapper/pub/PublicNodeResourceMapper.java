package net.statemesh.service.mapper.pub;

import net.statemesh.domain.NodeResource;
import net.statemesh.service.dto.pub.PublicNodeResourceDTO;
import net.statemesh.service.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PublicNodeResourceMapper extends EntityMapper<PublicNodeResourceDTO, NodeResource> {
}
