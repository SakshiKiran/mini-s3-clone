package com.example.mini_s3.model;

import lombok.Data;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Data
public class ObjectMetadata {
    private String bucket;
    private String key;
    private long size;
    private String contentType;
    private Instant uploadedAt;
    private String versionId;
    private Map<String, String> tags = new HashMap<>(); //Adding this for Object Tags Support
    private Instant expiresAt; //Adding this for Object Lifecycle management
    private String storageClass; // STANDARD, STANDARD_IA, etc.
    
    private Set<String> readAccess = new HashSet<>(); // Access Control Lists (ACLs)
    private Set<String> writeAccess = new HashSet<>();
}



