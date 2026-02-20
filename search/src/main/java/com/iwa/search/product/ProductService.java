package com.iwa.search.product;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductSearchRepository repository;

    ProductService(ProductSearchRepository repository) {
        this.repository = repository;
    }

    ProductDocument findById(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    List<ProductDocument> findAll() {
        return (List<ProductDocument>) repository.findAll();
    }

    List<ProductDocument> search(String query) {
        log.info("search_query={}", query);
        return findAll();
    }
}
