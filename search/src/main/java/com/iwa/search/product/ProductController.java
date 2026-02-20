package com.iwa.search.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
class ProductController {

    private final ProductService productService;
    private final ProductSearchMapper mapper;

    ProductController(ProductService productService, ProductSearchMapper mapper) {
        this.productService = productService;
        this.mapper = mapper;
    }

    @GetMapping
    ResponseEntity<Page<ProductSearchResponse>> findAll(Pageable pageable) {
        Page<ProductDocument> documents = productService.findAll(pageable);
        Page<ProductSearchResponse> response = documents.map(mapper::toResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    ResponseEntity<ProductSearchResponse> get(@PathVariable String id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @PostMapping("/search")
    ResponseEntity<Page<ProductSearchResponse>> search(@RequestBody ProductSearchRequest request, Pageable pageable) {
        Page<ProductDocument> documents = productService.search(request.query(), pageable);
        Page<ProductSearchResponse> response = documents.map(mapper::toResponse);
        return ResponseEntity.ok(response);
    }
}
