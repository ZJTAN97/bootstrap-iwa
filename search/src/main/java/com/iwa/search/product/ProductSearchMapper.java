package com.iwa.search.product;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductSearchMapper {

    ProductSearchResponse toResponse(ProductDocument document);

    java.util.List<ProductSearchResponse> toResponseList(java.util.List<ProductDocument> documents);
}
