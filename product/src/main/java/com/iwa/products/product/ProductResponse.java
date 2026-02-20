package com.iwa.products.product;

import java.util.List;

public record ProductResponse(String id, String samAccountName, String appointment, List<String> emailAddresses) {}
