package com.iwa.indexer.product;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

public interface ProductElasticsearchRepository extends ElasticsearchRepository<ProductDocument, String> {}
