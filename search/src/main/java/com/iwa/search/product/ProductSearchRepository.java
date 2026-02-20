package com.iwa.search.product;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {}
