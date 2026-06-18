package com.iwa.products.configuration;

import com.iwa.products.storage.MinioProperties;
import io.minio.MinioClient;
import io.minio.errors.ServerException;
import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfiguration {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    /**
     * Retries only transient failures: MinIO {@link ServerException} (HTTP 5xx) and {@link
     * IOException} (connection resets, timeouts). {@code traversingCauses} catches these when wrapped.
     * Deterministic errors ({@code ErrorResponseException} for not-found / access-denied) are not
     * listed, so they propagate on the first attempt.
     */
    @Bean
    public RetryTemplate minioRetryTemplate(MinioProperties properties) {
        MinioProperties.Retry retry = properties.retry();
        return RetryTemplate.builder()
                .maxAttempts(retry.maxAttempts())
                .exponentialBackoff(
                        retry.initialBackoff().toMillis(),
                        retry.multiplier(),
                        retry.maxBackoff().toMillis())
                .retryOn(ServerException.class)
                .retryOn(IOException.class)
                .traversingCauses()
                .build();
    }
}
