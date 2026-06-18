package com.iwa.products.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Exercises {@link FileStorageService} against a real {@link TempDir} sandbox — no mocking, since the
 * service is a thin wrapper over the local file system. Covers happy paths plus the sandbox guards
 * (path traversal, zip-slip, base-directory protection).
 */
class FileStorageServiceTests {

    @TempDir
    Path baseDir;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(baseDir.toString());
        service.init();
    }

    @Test
    void initCreatesBaseDirectoryAndIsIdempotent() throws IOException {
        Path nested = baseDir.resolve("does-not-exist-yet");
        FileStorageService fresh = new FileStorageService(nested.toString());

        fresh.init();
        fresh.init();

        assertThat(Files.isDirectory(nested)).isTrue();
    }

    @Test
    void createPathJoinsSegmentsUnderBaseDir() {
        Path created = service.createPath("folder", "nested");

        assertThat(created).isEqualTo(baseDir.resolve("folder").resolve("nested"));
        assertThat(Files.isDirectory(created)).isTrue();
    }

    @Test
    void createPathIsIdempotent() {
        Path first = service.createPath("folder");
        Path second = service.createPath("folder");

        assertThat(first).isEqualTo(second);
        assertThat(Files.isDirectory(first)).isTrue();
    }

    @Test
    void createPathRejectsTraversalEscape() {
        assertThatExceptionOfType(FileStorageException.class)
                .isThrownBy(() -> service.createPath("..", "escaped"))
                .withMessageContaining("escapes base directory");
    }

    @Test
    void createPathRejectsAbsoluteSegment() {
        Path outside = baseDir.getParent().resolve("outside");

        assertThatExceptionOfType(FileStorageException.class)
                .isThrownBy(() -> service.createPath(outside.toString()))
                .withMessageContaining("escapes base directory");
    }

    @Test
    void createPathRejectsEmptyAndBlankSegments() {
        assertThatExceptionOfType(FileStorageException.class).isThrownBy(() -> service.createPath());
        assertThatExceptionOfType(FileStorageException.class).isThrownBy(() -> service.createPath("a", " "));
    }

    @Test
    void unzipExtractsEntriesIntoDestination() throws IOException {
        Path zip = baseDir.resolve("archive.zip");
        writeZip(zip, entry("root.txt", "root"), entry("sub/leaf.txt", "leaf"));

        Path dest = service.unzip(zip, "extracted");

        assertThat(dest).isEqualTo(baseDir.resolve("extracted"));
        assertThat(Files.readString(dest.resolve("root.txt"))).isEqualTo("root");
        assertThat(Files.readString(dest.resolve("sub/leaf.txt"))).isEqualTo("leaf");
    }

    @Test
    void unzipBlocksZipSlipEntries() throws IOException {
        Path zip = baseDir.resolve("evil.zip");
        writeZip(zip, entry("../escape.txt", "pwned"));

        assertThatExceptionOfType(FileStorageException.class)
                .isThrownBy(() -> service.unzip(zip, "extracted"))
                .withMessageContaining("zip-slip");
        assertThat(Files.exists(baseDir.resolve("escape.txt"))).isFalse();
    }

    @Test
    void unzipFailsWhenArchiveMissing() {
        assertThatExceptionOfType(FileStorageException.class)
                .isThrownBy(() -> service.unzip(baseDir.resolve("nope.zip"), "extracted"))
                .withMessageContaining("does not exist");
    }

    @Test
    void deleteFileRemovesDirectoryTreeRecursively() {
        Path tree = service.createPath("folder", "nested");

        service.deleteFile("folder");

        assertThat(Files.exists(tree)).isFalse();
        assertThat(Files.exists(baseDir.resolve("folder"))).isFalse();
    }

    @Test
    void deleteFileIsIdempotentWhenMissing() {
        service.deleteFile("never-existed");

        assertThat(Files.exists(baseDir.resolve("never-existed"))).isFalse();
    }

    @Test
    void deleteFileRefusesBaseDirectory() {
        assertThatExceptionOfType(FileStorageException.class)
                .isThrownBy(() -> service.deleteFile("."))
                .withMessageContaining("base directory");
        assertThat(Files.isDirectory(baseDir)).isTrue();
    }

    @Test
    void deleteFileRejectsTraversalEscape() {
        assertThatExceptionOfType(FileStorageException.class)
                .isThrownBy(() -> service.deleteFile("..", "something"))
                .withMessageContaining("escapes base directory");
    }

    private record ZipFileEntry(String name, String content) {}

    private static ZipFileEntry entry(String name, String content) {
        return new ZipFileEntry(name, content);
    }

    private static void writeZip(Path zip, ZipFileEntry... entries) throws IOException {
        try (OutputStream out = Files.newOutputStream(zip);
                ZipOutputStream zos = new ZipOutputStream(out)) {
            for (ZipFileEntry e : entries) {
                zos.putNextEntry(new ZipEntry(e.name()));
                zos.write(e.content().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
    }
}
