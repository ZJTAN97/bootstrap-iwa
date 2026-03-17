package com.iwa.products.product;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateProductRequest(
        @NotBlank(message = "SAM account name cannot be blank")
        String samAccountName,

        @NotBlank(message = "Appointment Title cannot be blank")
        String appointmentTitle,

        List<String> emailAddresses) {}
