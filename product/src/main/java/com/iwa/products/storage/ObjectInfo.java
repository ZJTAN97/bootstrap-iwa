package com.iwa.products.storage;

import java.time.ZonedDateTime;

public record ObjectInfo(
        String bucket, String object, long size, String etag, String contentType, ZonedDateTime lastModified) {}
