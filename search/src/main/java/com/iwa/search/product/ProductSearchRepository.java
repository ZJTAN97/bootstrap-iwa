package com.iwa.search.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {

    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"samAccountName\", \"appointment\"]}}")
    Page<ProductDocument> searchByQuery(String query, Pageable pageable);
}
