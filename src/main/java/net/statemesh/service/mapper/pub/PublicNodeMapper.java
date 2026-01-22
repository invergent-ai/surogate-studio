package net.statemesh.service.mapper.pub;

import net.statemesh.domain.Node;
import net.statemesh.service.dto.pub.PublicNodeDTO;
import net.statemesh.service.mapper.EntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PublicNodeMapper extends EntityMapper<PublicNodeDTO, Node> {
}
