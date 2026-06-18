package com.iwa.products.storage;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Manages a scratch area on the host file system, typically used to stage artifacts pulled from
 * {@link MinioService} (download a zip, unzip, process, clean up).
 *
 * <p>All operations are sandboxed: every path is resolved against the configured base directory and
 * any attempt to escape it (via {@code ..} segments, absolute paths, or zip-slip entries) fails with
 * {@link FileStorageException}. Callers therefore pass paths <em>relative</em> to the base directory.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path baseDir;

    public FileStorageService(
            @Value("${file-storage.base-dir:${java.io.tmpdir}/product-storage}") String baseDir) {
        this.baseDir = Paths.get(baseDir).toAbsolutePath().normalize();
    }

    /**
     * Creates the base directory if it does not already exist. Idempotent: a no-op when the directory
     * is already present. Runs on startup but is safe to call again at any time.
     */
    @PostConstruct
    void init() {
        try {
            Files.createDirectories(baseDir);
            log.info("File storage base directory ready at {}", baseDir);
        } catch (IOException e) {
            throw new FileStorageException("Failed to create base directory '%s'".formatted(baseDir), e);
        }
    }

    /**
     * Creates a directory (and any missing parents) under the base directory and returns its absolute
     * path. Path segments are joined in order, e.g. {@code createPath("folder", "nested")}. Idempotent.
     * The returned path is guaranteed to live inside the sandbox.
     */
    public Path createPath(String... parts) {
        Path target = resolve(parts);
        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            throw new FileStorageException("Failed to create directory '%s'".formatted(target), e);
        }
        return target;
    }

    /**
     * Extracts a zip archive into {@code destRelativePath} under the base directory, creating the
     * destination if needed. Protects against zip-slip: any entry that would resolve outside the
     * destination is rejected. Returns the absolute destination directory.
     */
    public Path unzip(Path zipFile, String... destParts) {
        if (!Files.isRegularFile(zipFile)) {
            throw new FileStorageException("Zip file '%s' does not exist or is not a regular file".formatted(zipFile));
        }
        Path dest = createPath(destParts);
        try (InputStream in = Files.newInputStream(zipFile);
                ZipInputStream zip = new ZipInputStream(in)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path resolved = dest.resolve(entry.getName()).normalize();
                if (!resolved.startsWith(dest)) {
                    throw new FileStorageException(
                            "Zip entry '%s' escapes destination '%s' (zip-slip)".formatted(entry.getName(), dest));
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(resolved);
                } else {
                    Files.createDirectories(resolved.getParent());
                    Files.copy(zip, resolved, StandardCopyOption.REPLACE_EXISTING);
                }
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to unzip '%s' into '%s'".formatted(zipFile, dest), e);
        }
        log.debug("Unzipped {} into {}", zipFile, dest);
        return dest;
    }

    /**
     * Deletes the file or directory tree at {@code relativePath} under the base directory. Idempotent
     * and recursive: removing a missing path succeeds, and directories are removed with their
     * contents. The base directory itself cannot be deleted.
     */
    public void deleteFile(String... parts) {
        Path target = resolve(parts);
        if (target.equals(baseDir)) {
            throw new FileStorageException("Refusing to delete the base directory '%s'".formatted(baseDir));
        }
        if (!Files.exists(target)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(target)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new FileStorageException("Failed to delete '%s'".formatted(path), e);
                }
            });
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete '%s'".formatted(target), e);
        }
        log.debug("Deleted {}", target);
    }

    /** Joins caller-supplied path segments against the base directory and enforces the sandbox. */
    private Path resolve(String... parts) {
        if (parts == null || parts.length == 0) {
            throw new FileStorageException("Path must not be empty");
        }
        Path resolved = baseDir;
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                throw new FileStorageException("Path segment must not be blank");
            }
            resolved = resolved.resolve(part);
        }
        resolved = resolved.normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new FileStorageException(
                    "Path '%s' escapes base directory '%s'".formatted(String.join("/", parts), baseDir));
        }
        return resolved;
    }
}
