package com.iwa.search.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "products")
public record ProductDocument(
        @Id String id,
        @Field(type = FieldType.Text) String samAccountName,
        @Field(type = FieldType.Text) String appointment) {}
