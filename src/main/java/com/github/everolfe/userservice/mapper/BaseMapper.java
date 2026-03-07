package com.github.everolfe.userservice.mapper;

import java.util.List;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
        componentModel = ComponentModel.SPRING,
        builder = @org.mapstruct.Builder(disableBuilder = true),
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
public interface BaseMapper<E,D> {

    D toDto(E e);

    E toEntity(D d);

    List<D> toDtos(List<E> e);

    List<E> toEntities(List<D> d);
}
