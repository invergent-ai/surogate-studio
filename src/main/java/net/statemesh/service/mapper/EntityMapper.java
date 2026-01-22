package net.statemesh.service.mapper;

import java.util.List;

import org.mapstruct.*;

/**
 * Contract for a generic dto to entity mapper.
 *
 * @param <D> - DTO type parameter.
 * @param <E> - Entity type parameter.
 */

public interface EntityMapper<D, E> {
    E toEntity(D dto, @Context CycleAvoidingMappingContext cycleAvoidingMappingContext);
    D toDto(E entity, @Context CycleAvoidingMappingContext cycleAvoidingMappingContext);
    List<E> toEntity(List<D> dtoList, @Context CycleAvoidingMappingContext cycleAvoidingMappingContext);
    List<D> toDto(List<E> entityList, @Context CycleAvoidingMappingContext cycleAvoidingMappingContext);

    @Named("partialUpdate")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void partialUpdate(@MappingTarget E entity, D dto);
}
