package com.iwa.search.product;

import java.util.List;

public record ProductSearchResponse(
        String id, String samAccountName, String appointment, List<String> emailAddresses) {}
