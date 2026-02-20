package com.iwa.products.product;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
interface ProductRepository extends MongoRepository<Product, ObjectId> {}
