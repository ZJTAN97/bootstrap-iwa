package com.iwa.products.product;

import java.util.List;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;

    ProductService(
            ProductRepository repository,
            RabbitTemplate rabbitTemplate,
            @Value("${product.rabbitmq.exchange}") String exchange) {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
    }

    Product create(Product product) {
        Product saved = repository.save(product);
        log.info("product_created id={}", saved.id());
        publishEvent(saved.id().toString(), saved, ProductEvent.EventType.CREATED);
        return saved;
    }

    Product update(ObjectId id, Product product) {
        Product updated = new Product(id, product.samAccountName(), product.appointment(), product.emailAddresses());
        Product saved = repository.save(updated);
        log.info("product_updated id={}", saved.id());
        publishEvent(saved.id().toString(), saved, ProductEvent.EventType.UPDATED);
        return saved;
    }

    void delete(ObjectId id) {
        repository.deleteById(id);
        log.info("product_deleted id={}", id);
        ProductEvent event = new ProductEvent(id.toString(), null, null, null, ProductEvent.EventType.DELETED);
        String routingKey = "product.deleted";
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("event_published productId={} eventType=DELETED", id);
    }

    Product findById(ObjectId id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    List<Product> findAll() {
        return repository.findAll();
    }

    private void publishEvent(String productId, Product product, ProductEvent.EventType eventType) {
        ProductEvent event = new ProductEvent(
                productId, product.samAccountName(), product.appointment(), product.emailAddresses(), eventType);
        String routingKey = "product." + eventType.name().toLowerCase();
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
        log.info("event_published productId={} eventType={}", productId, eventType);
    }
}
