package com.iwa.products.storage;

/**
 * Unchecked exception for local file system failures raised by {@link FileStorageService}. Covers
 * I/O errors as well as sandbox violations (path traversal / zip-slip).
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
