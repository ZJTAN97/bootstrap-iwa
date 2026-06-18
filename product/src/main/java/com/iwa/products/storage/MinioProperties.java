package com.iwa.products.storage;

import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        @NotBlank String endpoint,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        @DefaultValue Retry retry) {

    /**
     * Retry policy for transient MinIO failures (5xx / connection errors). Not-found and other
     * deterministic 4xx errors are never retried.
     */
    public record Retry(
            @DefaultValue("3") int maxAttempts,
            @DefaultValue("200ms") Duration initialBackoff,
            @DefaultValue("2.0") double multiplier,
            @DefaultValue("2s") Duration maxBackoff) {}
}
