package com.iwa.products.product;

import jakarta.validation.Valid;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/products")
class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final ProductMapper productMapper;

    ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @GetMapping
    ResponseEntity<Page<ProductResponse>> list(Pageable pageable) {
        log.debug("Fetching products page {} size {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<ProductResponse> products = productService.findAll(pageable).map(productMapper::toResponse);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{id}")
    ResponseEntity<ProductResponse> get(@PathVariable String id) {
        log.debug("Fetching product by id: {}", id);
        Product product = productService.findById(new ObjectId(id));
        return ResponseEntity.ok(productMapper.toResponse(product));
    }

    @PostMapping
    ResponseEntity<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        log.info("Creating product with samAccountName: {}", request.samAccountName());
        Product product = productMapper.toEntity(request);
        Product created = productService.create(product);
        ProductResponse response = productMapper.toResponse(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    ResponseEntity<ProductResponse> update(@PathVariable String id, @Valid @RequestBody UpdateProductRequest request) {
        log.info("Updating product with id: {}", id);
        Product product = productMapper.toEntity(request);
        Product updated = productService.update(new ObjectId(id), product);
        return ResponseEntity.ok(productMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable String id) {
        log.info("Deleting product with id: {}", id);
        productService.delete(new ObjectId(id));
        return ResponseEntity.noContent().build();
    }
}
