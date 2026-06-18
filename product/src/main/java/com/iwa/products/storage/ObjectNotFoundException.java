package com.iwa.products.storage;

public class ObjectNotFoundException extends MinioStorageException {

    public ObjectNotFoundException(String bucket, String object, Throwable cause) {
        super("Object '%s' not found in bucket '%s'".formatted(object, bucket), cause);
    }
}
