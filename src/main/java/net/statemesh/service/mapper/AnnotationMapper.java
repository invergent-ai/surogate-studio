package net.statemesh.service.mapper;

import net.statemesh.domain.Annotation;
import net.statemesh.service.dto.AnnotationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper for the entity {@link Annotation} and its DTO {@link AnnotationDTO}.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AnnotationMapper extends EntityMapper<AnnotationDTO, Annotation> {
}
