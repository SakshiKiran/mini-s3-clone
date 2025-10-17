package com.example.mini_s3.model;

import lombok.Data;
import java.time.Instant;

@Data
public class ObjectMetadata {
    private String bucket;
    private String key;
    private long size;
    private String contentType;
    private Instant uploadedAt;
    private String versionId;
}
