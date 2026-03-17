package com.iwa.products.configuration;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
        componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.ERROR, // Fail build if fields are missing
        collectionMappingStrategy = CollectionMappingStrategy.ADDER_PREFERRED)
public class MapstructConfiguration {}
