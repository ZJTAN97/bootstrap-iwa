package com.iwa.search.product;

import org.springframework.web.bind.annotation.RestController;

@RestController
class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }
}
