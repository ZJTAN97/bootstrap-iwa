package com.iwa.products.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.minio.MinioClient;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.retry.support.RetryTemplate;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class MinioServiceIntegrationTests {

    private static final String BUCKET = "test-bucket";

    @Container
    private static final MinIOContainer MINIO = new MinIOContainer("minio/minio:RELEASE.2025-04-22T22-12-26Z");

    private static MinioService minioService;

    @TempDir
    private Path tempDir;

    @BeforeAll
    static void setUp() {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(MINIO.getS3URL())
                .credentials(MINIO.getUserName(), MINIO.getPassword())
                .build();
        minioService = new MinioService(
                minioClient, RetryTemplate.builder().maxAttempts(1).build());
        minioService.makeBucket(BUCKET);
    }

    @Test
    void uploadObjectThenStatObjectReturnsMetadata() throws Exception {
        Path file = Files.writeString(tempDir.resolve("stat-me.txt"), "hello minio");
        String objectPath = randomObjectPath();

        minioService.uploadObject(BUCKET, objectPath, file);
        ObjectInfo info = minioService.statObject(BUCKET, objectPath);

        assertThat(info.bucket()).isEqualTo(BUCKET);
        assertThat(info.object()).isEqualTo(objectPath);
        assertThat(info.size()).isEqualTo("hello minio".getBytes(StandardCharsets.UTF_8).length);
        assertThat(info.etag()).isNotBlank();
        assertThat(info.lastModified()).isNotNull();
    }

    @Test
    void getObjectStreamsUploadedContent() throws Exception {
        Path file = Files.writeString(tempDir.resolve("get-me.txt"), "stream content");
        String objectPath = randomObjectPath();
        minioService.uploadObject(BUCKET, objectPath, file);

        try (InputStream stream = minioService.getObject(BUCKET, objectPath)) {
            assertThat(new String(stream.readAllBytes(), StandardCharsets.UTF_8))
                    .isEqualTo("stream content");
        }
    }

    @Test
    void downloadObjectWritesToLocalPathAndCreatesParentDirectories() throws Exception {
        Path file = Files.writeString(tempDir.resolve("download-me.txt"), "download content");
        String objectPath = randomObjectPath();
        minioService.uploadObject(BUCKET, objectPath, file);

        Path target = tempDir.resolve("nested/dirs/downloaded.txt");
        minioService.downloadObject(BUCKET, objectPath, target);

        assertThat(target).exists();
        assertThat(Files.readString(target)).isEqualTo("download content");
    }

    @Test
    void putObjectUploadsFromStream() throws Exception {
        byte[] content = "from a stream".getBytes(StandardCharsets.UTF_8);
        String objectPath = randomObjectPath();

        minioService.putObject(BUCKET, objectPath, new ByteArrayInputStream(content), content.length, "text/plain");

        ObjectInfo info = minioService.statObject(BUCKET, objectPath);
        assertThat(info.size()).isEqualTo(content.length);
        assertThat(info.contentType()).isEqualTo("text/plain");
    }

    @Test
    void statObjectOnMissingObjectThrowsObjectNotFound() {
        assertThatExceptionOfType(ObjectNotFoundException.class)
                .isThrownBy(() -> minioService.statObject(BUCKET, "does/not/exist.txt"))
                .withMessageContaining("does/not/exist.txt");
    }

    @Test
    void getObjectOnMissingObjectThrowsObjectNotFound() {
        assertThatExceptionOfType(ObjectNotFoundException.class)
                .isThrownBy(() -> minioService.getObject(BUCKET, "missing.bin"));
    }

    @Test
    void uploadObjectToMissingBucketThrowsBucketNotFound() throws Exception {
        Path file = Files.writeString(tempDir.resolve("orphan.txt"), "no home");

        assertThatExceptionOfType(BucketNotFoundException.class)
                .isThrownBy(() -> minioService.uploadObject("no-such-bucket", "orphan.txt", file))
                .withMessageContaining("no-such-bucket");
    }

    @Test
    void uploadObjectFromMissingLocalFileThrowsStorageException() {
        assertThatExceptionOfType(MinioStorageException.class)
                .isThrownBy(() -> minioService.uploadObject(BUCKET, "ghost.txt", tempDir.resolve("ghost.txt")));
    }

    @Test
    void removeObjectDeletesObject() throws Exception {
        Path file = Files.writeString(tempDir.resolve("remove-me.txt"), "temporary");
        String objectPath = randomObjectPath();
        minioService.uploadObject(BUCKET, objectPath, file);

        minioService.removeObject(BUCKET, objectPath);

        assertThatExceptionOfType(ObjectNotFoundException.class)
                .isThrownBy(() -> minioService.statObject(BUCKET, objectPath));
    }

    @Test
    void listObjectsFiltersByPrefix() throws Exception {
        Path file = Files.writeString(tempDir.resolve("list-me.txt"), "listed");
        String prefix = "list-test/" + UUID.randomUUID() + "/";
        minioService.uploadObject(BUCKET, prefix + "a.txt", file);
        minioService.uploadObject(BUCKET, prefix + "sub/b.txt", file);
        minioService.uploadObject(BUCKET, "other/" + UUID.randomUUID() + ".txt", file);

        List<ObjectInfo> objects = minioService.listObjects(BUCKET, prefix);

        assertThat(objects)
                .extracting(ObjectInfo::object)
                .containsExactlyInAnyOrder(prefix + "a.txt", prefix + "sub/b.txt");
    }

    @Test
    void removeFolderDeletesAllObjectsRecursively() throws Exception {
        Path file = Files.writeString(tempDir.resolve("folder-me.txt"), "in folder");
        String folder = "remove-folder/" + UUID.randomUUID();
        minioService.uploadObject(BUCKET, folder + "/a.txt", file);
        minioService.uploadObject(BUCKET, folder + "/sub/b.txt", file);
        minioService.uploadObject(BUCKET, folder + "/sub/deep/c.txt", file);

        minioService.removeFolder(BUCKET, folder);

        assertThat(minioService.listObjects(BUCKET, folder)).isEmpty();
    }

    @Test
    void removeFolderLeavesSiblingObjectsIntact() throws Exception {
        Path file = Files.writeString(tempDir.resolve("sibling-me.txt"), "sibling");
        String base = "sibling-test/" + UUID.randomUUID();
        minioService.uploadObject(BUCKET, base + "/target/a.txt", file);
        // Same prefix string but a different folder — must NOT be deleted.
        minioService.uploadObject(BUCKET, base + "/target-archive/b.txt", file);

        minioService.removeFolder(BUCKET, base + "/target");

        assertThat(minioService.listObjects(BUCKET, base))
                .extracting(ObjectInfo::object)
                .containsExactly(base + "/target-archive/b.txt");
    }

    @Test
    void removeFolderOnMissingFolderIsNoOp() {
        // No objects under the prefix — should succeed without throwing.
        minioService.removeFolder(BUCKET, "no-such-folder/" + UUID.randomUUID());
    }

    @Test
    void removeFolderRejectsBlankBucket() {
        assertThatExceptionOfType(MinioStorageException.class)
                .isThrownBy(() -> minioService.removeFolder("  ", "some-folder"))
                .withMessageContaining("bucketName");
    }

    @Test
    void removeFolderRejectsBlankFolder() {
        assertThatExceptionOfType(MinioStorageException.class)
                .isThrownBy(() -> minioService.removeFolder(BUCKET, ""))
                .withMessageContaining("folderToDelete");
    }

    @Test
    void presignedGetUrlIsDownloadableWithoutCredentials() throws Exception {
        Path file = Files.writeString(tempDir.resolve("presign-me.txt"), "presigned content");
        String objectPath = randomObjectPath();
        minioService.uploadObject(BUCKET, objectPath, file);

        String url = minioService.presignedGetUrl(BUCKET, objectPath, Duration.ofMinutes(5));

        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpResponse<String> response = httpClient.send(
                    HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("presigned content");
        }
    }

    @Test
    void bucketExistsAndMakeBucket() {
        String newBucket = "bucket-" + UUID.randomUUID();

        assertThat(minioService.bucketExists(newBucket)).isFalse();
        minioService.makeBucket(newBucket);
        assertThat(minioService.bucketExists(newBucket)).isTrue();
    }

    private static String randomObjectPath() {
        return "objects/" + UUID.randomUUID() + ".txt";
    }
}
