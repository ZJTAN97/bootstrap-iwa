package com.iwa.indexer.listener;

import com.iwa.indexer.product.ProductEvent;
import com.iwa.indexer.product.ProductIndexingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    private final ProductIndexingService indexingService;

    public ProductEventListener(ProductIndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @RabbitListener(queues = "${indexer.rabbitmq.queue}")
    public void handleProductEvent(ProductEvent event) {
        log.info("received_product_event eventType={} productId={}", event.eventType(), event.id());
        try {
            indexingService.index(event);
        } catch (Exception e) {
            log.error("failed_to_index_product productId={}", event.id(), e);
            throw e;
        }
    }
}
