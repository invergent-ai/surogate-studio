package net.statemesh.service.mapper;

import net.statemesh.domain.Node;
import net.statemesh.service.dto.NodeDTO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.Optional;

/**
 * Mapper for the entity {@link Node} and its DTO {@link NodeDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NodeMapper extends EntityMapper<NodeDTO, Node> {
    @AfterMapping
    default void setNode(@MappingTarget Node node) {
        Optional.ofNullable(node.getHistory())
            .ifPresent(h -> h.forEach(item -> item.setNode(node)));
    }
}
