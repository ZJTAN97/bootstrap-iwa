package com.iwa.products.product;

import java.util.List;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("product")
public record Product(@Id ObjectId id, String samAccountName, String appointment, List<String> emailAddresses) {}
