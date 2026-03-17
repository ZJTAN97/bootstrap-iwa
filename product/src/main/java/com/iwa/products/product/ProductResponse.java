package com.iwa.products.product;

import java.util.List;

public record ProductResponse(
        String id, String samAccountName, List<String> emailAddresses, String appointmentTitle, int rankNumber) {}
