package com.iwa.products.product;

import com.iwa.products.configuration.MapstructConfiguration;
import org.bson.types.ObjectId;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(config = MapstructConfiguration.class)
public interface ProductMapper {

    @Named("objectIdToString")
    default String objectIdToString(ObjectId objectId) {
        return objectId != null ? objectId.toString() : null;
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "appointmentTitle", target = "appointment.title")
    Product toEntity(CreateProductRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(source = "appointmentTitle", target = "appointment.title")
    Product toEntity(UpdateProductRequest request);

    @Mapping(source = "id", target = "id", qualifiedByName = "objectIdToString")
    @Mapping(source = "appointment.title", target = "appointmentTitle")
    @Mapping(source = "appointment.rankNumber", target = "rankNumber")
    ProductResponse toResponse(Product entity);
}
