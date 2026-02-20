package com.iwa.products.product;

import java.util.List;

public record ProductEvent(
        String id, String samAccountName, String appointment, List<String> emailAddresses, EventType eventType) {

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}
