package com.iwa.products.storage;

public class BucketNotFoundException extends MinioStorageException {

    public BucketNotFoundException(String bucket, Throwable cause) {
        super("Bucket '%s' does not exist".formatted(bucket), cause);
    }
}
