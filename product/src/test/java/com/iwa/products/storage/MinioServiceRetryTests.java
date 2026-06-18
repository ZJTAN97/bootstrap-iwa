package com.iwa.products.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.ServerException;
import io.minio.messages.ErrorResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.support.RetryTemplate;

/**
 * Verifies retry behaviour with a mocked {@link MinioClient} — fast and deterministic, no container.
 * The template mirrors production (retry on {@link ServerException}/{@link IOException}) but with no
 * backoff so the test does not sleep.
 */
@ExtendWith(MockitoExtension.class)
class MinioServiceRetryTests {

    private static final String BUCKET = "bucket";
    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private MinioClient minioClient;

    private MinioService minioService;

    @BeforeEach
    void setUp() {
        RetryTemplate retryTemplate = RetryTemplate.builder()
                .maxAttempts(MAX_ATTEMPTS)
                .noBackoff()
                .retryOn(ServerException.class)
                .retryOn(IOException.class)
                .traversingCauses()
                .build();
        minioService = new MinioService(minioClient, retryTemplate);
    }

    @Test
    void retriesTransientIoExceptionThenSucceeds() throws Exception {
        StatObjectResponse response = mock(StatObjectResponse.class);
        when(minioClient.statObject(any(StatObjectArgs.class)))
                .thenThrow(new IOException("connection reset"))
                .thenThrow(new IOException("connection reset"))
                .thenReturn(response);

        ObjectInfo info = minioService.statObject(BUCKET, "object.txt");

        assertThat(info.bucket()).isEqualTo(BUCKET);
        verify(minioClient, times(MAX_ATTEMPTS)).statObject(any(StatObjectArgs.class));
    }

    @Test
    void exhaustsRetriesThenThrowsStorageException() throws Exception {
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(new IOException("down"));

        assertThatExceptionOfType(MinioStorageException.class)
                .isThrownBy(() -> minioService.statObject(BUCKET, "object.txt"));

        verify(minioClient, times(MAX_ATTEMPTS)).statObject(any(StatObjectArgs.class));
    }

    @Test
    void doesNotRetryDeterministicNotFound() throws Exception {
        ErrorResponseException notFound = mock(ErrorResponseException.class);
        when(notFound.errorResponse())
                .thenReturn(new ErrorResponse(
                        "NoSuchKey", "Object does not exist", BUCKET, "missing.txt", null, null, null));
        when(minioClient.statObject(any(StatObjectArgs.class))).thenThrow(notFound);

        assertThatExceptionOfType(ObjectNotFoundException.class)
                .isThrownBy(() -> minioService.statObject(BUCKET, "missing.txt"));

        verify(minioClient, times(1)).statObject(any(StatObjectArgs.class));
    }

    @Test
    void doesNotRetryStreamUploadEvenOnTransientError() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new IOException("flaky"));

        assertThatExceptionOfType(MinioStorageException.class)
                .isThrownBy(() -> minioService.putObject(
                        BUCKET, "object.txt", new ByteArrayInputStream("data".getBytes()), 4, "text/plain"));

        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }
}
