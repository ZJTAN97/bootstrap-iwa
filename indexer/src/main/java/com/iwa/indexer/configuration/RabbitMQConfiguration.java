package com.iwa.indexer.configuration;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {

    private final RabbitMQProperties properties;

    public RabbitMQConfiguration(RabbitMQProperties properties) {
        this.properties = properties;
    }

    @Bean
    public TopicExchange productExchange() {
        return new TopicExchange(properties.getExchange());
    }

    @Bean
    public Queue productIndexQueue() {
        return new Queue(properties.getQueue(), true);
    }

    @Bean
    public Binding binding(Queue productIndexQueue, TopicExchange productExchange) {
        return BindingBuilder.bind(productIndexQueue).to(productExchange).with(properties.getRoutingKey());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
