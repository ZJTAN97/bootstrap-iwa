package com.iwa.products.product;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record UpdateProductRequest(
        @NotBlank(message = "SAM account name cannot be blank")
        String samAccountName,

        @NotBlank(message = "Appointment cannot be blank") String appointment,
        List<String> emailAddresses) {}
