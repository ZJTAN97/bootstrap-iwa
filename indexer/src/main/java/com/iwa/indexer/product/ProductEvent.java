package com.iwa.indexer.product;

import java.util.List;

public record ProductEvent(
        String id, String samAccountName, String appointment, List<String> emailAddresses, EventType eventType) {

    public enum EventType {
        CREATED,
        UPDATED,
        DELETED
    }
}
