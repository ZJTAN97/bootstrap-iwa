package com.iwa.products.storage;

import io.minio.BucketExistsArgs;
import io.minio.DownloadObjectArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.RemoveObjectsArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.UploadObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Thin wrapper around {@link MinioClient} that translates the SDK's checked exceptions into the
 * unchecked {@link MinioStorageException} hierarchy. Buckets are never created implicitly; writes
 * against a missing bucket fail with {@link BucketNotFoundException}.
 */
@Service
public class MinioService {

    private static final Logger log = LoggerFactory.getLogger(MinioService.class);

    private static final Set<String> OBJECT_NOT_FOUND_CODES = Set.of("NoSuchKey", "NoSuchObject");
    private static final String BUCKET_NOT_FOUND_CODE = "NoSuchBucket";
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final MinioClient minioClient;
    private final RetryTemplate retryTemplate;

    public MinioService(MinioClient minioClient, RetryTemplate minioRetryTemplate) {
        this.minioClient = minioClient;
        this.retryTemplate = minioRetryTemplate;
    }

    /**
     * Streams the object's content. The caller owns the returned stream and must close it.
     */
    public InputStream getObject(String bucket, String objectPath) {
        return execute(
                bucket,
                objectPath,
                "getObject",
                true,
                () -> minioClient.getObject(GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .build()));
    }

    public ObjectInfo statObject(String bucket, String objectPath) {
        StatObjectResponse stat = execute(
                bucket,
                objectPath,
                "statObject",
                true,
                () -> minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectPath)
                        .build()));
        return new ObjectInfo(bucket, objectPath, stat.size(), stat.etag(), stat.contentType(), stat.lastModified());
    }

    /**
     * Downloads the object to {@code downloadPath} on the local file system, creating parent
     * directories as needed. An existing file at the destination is overwritten.
     */
    public void downloadObject(String bucket, String objectPath, Path downloadPath) {
        Path parent = downloadPath.toAbsolutePath().getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new MinioStorageException("Failed to create download directory '%s'".formatted(parent), e);
            }
        }
        execute(bucket, objectPath, "downloadObject", true, () -> {
            minioClient.downloadObject(DownloadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .filename(downloadPath.toString())
                    .overwrite(true)
                    .build());
            return null;
        });
        log.debug("Downloaded {}/{} to {}", bucket, objectPath, downloadPath);
    }

    /**
     * Uploads the file at {@code uploadPath} from the local file system. Content type is probed
     * from the file name and falls back to {@code application/octet-stream}.
     */
    public void uploadObject(String bucket, String objectPath, Path uploadPath) {
        if (!Files.isRegularFile(uploadPath)) {
            throw new MinioStorageException(
                    "Local file '%s' does not exist or is not a regular file".formatted(uploadPath));
        }
        execute(bucket, objectPath, "uploadObject", true, () -> {
            minioClient.uploadObject(UploadObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .filename(uploadPath.toString())
                    .contentType(probeContentType(uploadPath))
                    .build());
            return null;
        });
        log.debug("Uploaded {} to {}/{}", uploadPath, bucket, objectPath);
    }

    /**
     * Uploads from a stream when the content does not live on disk. The stream is not closed by
     * this method.
     */
    public void putObject(String bucket, String objectPath, InputStream stream, long size, String contentType) {
        execute(
                bucket,
                objectPath,
                "putObject",
                false,
                () -> minioClient.putObject(
                        PutObjectArgs.builder().bucket(bucket).object(objectPath).stream(stream, size, -1)
                                .contentType(contentType != null ? contentType : DEFAULT_CONTENT_TYPE)
                                .build()));
    }

    /**
     * Removes the object. MinIO delete is idempotent: removing a missing object succeeds.
     */
    public void removeObject(String bucket, String objectPath) {
        execute(bucket, objectPath, "removeObject", true, () -> {
            minioClient.removeObject(
                    RemoveObjectArgs.builder().bucket(bucket).object(objectPath).build());
            return null;
        });
    }

    /**
     * Deletes every object under {@code folderToDelete} (recursive). MinIO has no native folder
     * delete: keys are flat and a "folder" is just a key prefix. Lists keys under the prefix and
     * removes them in bulk. A missing folder (no objects under the prefix) is a no-op and succeeds.
     * {@code bucketName} and {@code folderToDelete} must be non-null and non-blank, otherwise a
     * {@link MinioStorageException} is thrown — guards against accidentally wiping a whole bucket.
     */
    public void removeFolder(String bucketName, String folderToDelete) {
        if (!StringUtils.hasText(bucketName)) {
            throw new MinioStorageException("bucketName must not be null or blank");
        }
        if (!StringUtils.hasText(folderToDelete)) {
            throw new MinioStorageException("folderToDelete must not be null or blank");
        }
        String prefix = folderToDelete.endsWith("/") ? folderToDelete : folderToDelete + "/";

        execute(bucketName, prefix, "removeFolder", false, () -> {
            Iterable<Result<Item>> listed = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(prefix)
                    .recursive(true)
                    .build());

            List<DeleteObject> toDelete = new ArrayList<>();
            for (Result<Item> r : listed) {
                toDelete.add(new DeleteObject(r.get().objectName()));
            }
            if (toDelete.isEmpty()) {
                return null;
            }

            Iterable<Result<DeleteError>> errors = minioClient.removeObjects(RemoveObjectsArgs.builder()
                    .bucket(bucketName)
                    .objects(toDelete)
                    .build());

            // removeObjects is lazy — deletions fire only as this iterates.
            List<String> failed = new ArrayList<>();
            for (Result<DeleteError> e : errors) {
                DeleteError err = e.get();
                failed.add("%s: %s".formatted(err.objectName(), err.message()));
            }
            if (!failed.isEmpty()) {
                throw new MinioStorageException("Failed to delete %d object(s) under '%s/%s': %s"
                        .formatted(failed.size(), bucketName, prefix, failed));
            }
            return null;
        });
        log.debug("Removed folder {}/{}", bucketName, prefix);
    }

    /**
     * Lists all objects under {@code prefix} (recursive). Pass an empty or null prefix to list the
     * whole bucket. Content type is not available from listings and is null on returned entries.
     */
    public List<ObjectInfo> listObjects(String bucket, String prefix) {
        return execute(bucket, prefix, "listObjects", true, () -> {
            Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix != null ? prefix : "")
                    .recursive(true)
                    .build());
            List<ObjectInfo> objects = new ArrayList<>();
            for (Result<Item> result : results) {
                Item item = result.get();
                objects.add(
                        new ObjectInfo(bucket, item.objectName(), item.size(), item.etag(), null, item.lastModified()));
            }
            return objects;
        });
    }

    public String presignedGetUrl(String bucket, String objectPath, Duration expiry) {
        return presignedUrl(Method.GET, bucket, objectPath, expiry);
    }

    public String presignedPutUrl(String bucket, String objectPath, Duration expiry) {
        return presignedUrl(Method.PUT, bucket, objectPath, expiry);
    }

    public boolean bucketExists(String bucket) {
        return execute(
                bucket,
                null,
                "bucketExists",
                true,
                () -> minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build()));
    }

    public void makeBucket(String bucket) {
        execute(bucket, null, "makeBucket", false, () -> {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            return null;
        });
    }

    private String presignedUrl(Method method, String bucket, String objectPath, Duration expiry) {
        return execute(
                bucket,
                objectPath,
                "presignedUrl",
                true,
                () -> minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                        .method(method)
                        .bucket(bucket)
                        .object(objectPath)
                        .expiry((int) expiry.toSeconds(), TimeUnit.SECONDS)
                        .build()));
    }

    private String probeContentType(Path file) {
        try {
            String contentType = Files.probeContentType(file);
            return contentType != null ? contentType : DEFAULT_CONTENT_TYPE;
        } catch (IOException e) {
            return DEFAULT_CONTENT_TYPE;
        }
    }

    /**
     * Runs a MinIO call, translating SDK exceptions into the {@link MinioStorageException} hierarchy.
     * When {@code retryable} is true the call is retried on transient failures by the {@link
     * RetryTemplate} (the classifier sees the raw SDK exception, before translation). Stream-consuming
     * and non-idempotent operations pass {@code false}.
     */
    private <T> T execute(String bucket, String objectPath, String operation, boolean retryable, MinioCall<T> call) {
        try {
            return retryable ? retryTemplate.execute(ctx -> call.call()) : call.call();
        } catch (ErrorResponseException e) {
            throw translate(bucket, objectPath, operation, e);
        } catch (Exception e) {
            throw new MinioStorageException(
                    "MinIO %s failed for bucket '%s', object '%s'".formatted(operation, bucket, objectPath), e);
        }
    }

    private MinioStorageException translate(
            String bucket, String objectPath, String operation, ErrorResponseException e) {
        String code = e.errorResponse().code();
        if (BUCKET_NOT_FOUND_CODE.equals(code)) {
            return new BucketNotFoundException(bucket, e);
        }
        if (OBJECT_NOT_FOUND_CODES.contains(code)) {
            return new ObjectNotFoundException(bucket, objectPath, e);
        }
        return new MinioStorageException(
                "MinIO %s failed for bucket '%s', object '%s' with error code '%s'"
                        .formatted(operation, bucket, objectPath, code),
                e);
    }

    @FunctionalInterface
    private interface MinioCall<T> {
        T call() throws Exception;
    }
}
