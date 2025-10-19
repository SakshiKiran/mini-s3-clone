package com.example.mini_s3.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ListObjectsResult {
    private List<ObjectMetadata> objects;
    private List<String> commonPrefixes; // For delimiter-based listing (folder-like structure)
    private String nextContinuationToken;
    private boolean truncated;
    private String bucket;
    private String prefix;
    private String delimiter;
    private int maxKeys;
    
    public ListObjectsResult() {
        this.objects = new ArrayList<>();
        this.commonPrefixes = new ArrayList<>();
        this.truncated = false;
        this.maxKeys = 1000; // Default value like AWS S3
    }

    public ListObjectsResult(String bucket, String prefix, String delimiter, int maxKeys) {
        this();
        this.bucket = bucket;
        this.prefix = prefix;
        this.delimiter = delimiter;
        this.maxKeys = maxKeys > 0 ? maxKeys : this.maxKeys;
    }

    // Utility method to check if there are more results
    public boolean hasMoreResults() {
        return truncated && nextContinuationToken != null;
    }

    // Utility method to check if result is empty
    public boolean isEmpty() {
        return objects.isEmpty() && commonPrefixes.isEmpty();
    }

    // Utility method to get total number of entries (objects + prefixes)
    public int getTotalEntries() {
        return objects.size() + commonPrefixes.size();
    }
}
