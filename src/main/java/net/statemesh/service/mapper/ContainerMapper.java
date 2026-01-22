package net.statemesh.service.mapper;

import net.statemesh.domain.Container;
import net.statemesh.service.dto.ContainerDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Container} and its DTO {@link ContainerDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ContainerMapper extends EntityMapper<ContainerDTO, Container> {
}
