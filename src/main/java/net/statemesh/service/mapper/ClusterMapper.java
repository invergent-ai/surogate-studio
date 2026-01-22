package net.statemesh.service.mapper;

import net.statemesh.domain.Cluster;
import net.statemesh.service.dto.ClusterDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Cluster} and its DTO {@link ClusterDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ClusterMapper extends EntityMapper<ClusterDTO, Cluster> {
}
