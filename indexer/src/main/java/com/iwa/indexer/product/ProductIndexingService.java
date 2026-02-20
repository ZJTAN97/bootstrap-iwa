package com.iwa.indexer.product;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductIndexingService {

    private static final Logger log = LoggerFactory.getLogger(ProductIndexingService.class);

    private final ProductElasticsearchRepository repository;

    public ProductIndexingService(ProductElasticsearchRepository repository) {
        this.repository = repository;
    }

    public void index(ProductEvent event) {
        log.info("index_product eventType={} productId={}", event.eventType(), event.id());

        if (event.eventType() == ProductEvent.EventType.DELETED) {
            repository.deleteById(event.id());
            log.info("product_index_deleted productId={}", event.id());
            return;
        }

        ProductDocument document =
                new ProductDocument(event.id(), event.samAccountName(), event.appointment(), event.emailAddresses());

        repository.save(document);
        log.info("product_indexed productId={}", event.id());
    }
}
