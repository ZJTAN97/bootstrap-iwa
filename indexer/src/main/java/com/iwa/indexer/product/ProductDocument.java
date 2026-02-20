package com.iwa.indexer.product;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "products")
record ProductDocument(
        @Id String id,
        @Field(type = FieldType.Text) String samAccountName,
        @Field(type = FieldType.Text) String appointment) {}
