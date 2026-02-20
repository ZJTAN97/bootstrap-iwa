package com.iwa.search.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductSearchRepository repository;

    ProductService(ProductSearchRepository repository) {
        this.repository = repository;
    }

    ProductSearchResponse findById(String id) {
        return repository
                .findById(id)
                .map(doc -> new ProductSearchResponse(doc.id(), doc.samAccountName(), doc.appointment(), null))
                .orElseThrow(() -> new RuntimeException("Product not found"));
    }

    Page<ProductDocument> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    Page<ProductDocument> search(String query, Pageable pageable) {
        log.info("search_query={}", query);
        return repository.searchByQuery(query, pageable);
    }
}
