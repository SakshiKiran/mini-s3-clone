package com.example.mini_s3.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class BucketPolicy {
    private String bucket;
    
    // Basic access controls
    private boolean publicRead;
    private boolean publicWrite;
    
    // IP-based access control
    private List<String> allowedIPs = new ArrayList<>();
    
    // User-based access control
    private Set<String> readUsers = new HashSet<>();
    private Set<String> writeUsers = new HashSet<>();
    
    // Versioning configuration
    private boolean versioningEnabled;
    
    // Lifecycle rules
    private int objectExpirationDays; // 0 means no expiration
    
    // CORS configuration
    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedMethods = new ArrayList<>();
    
    // Server-side encryption
    private boolean encryptionEnabled;
    private String encryptionType; // "AES256" or "none"
    
    // Logging configuration
    private boolean loggingEnabled;
    private String logBucket; // Target bucket for logs
    
    // Website hosting configuration
    private boolean websiteEnabled;
    private String indexDocument;
    private String errorDocument;
    
    // Default constructor
    public BucketPolicy() {
        this.publicRead = false;
        this.publicWrite = false;
        this.versioningEnabled = false;
        this.objectExpirationDays = 0;
        this.encryptionEnabled = false;
        this.encryptionType = "none";
        this.loggingEnabled = false;
        this.websiteEnabled = false;
    }
    
    // Constructor with bucket name
    public BucketPolicy(String bucket) {
        this();
        this.bucket = bucket;
    }
    
    // Utility methods
    public void addAllowedIP(String ip) {
        if (!allowedIPs.contains(ip)) {
            allowedIPs.add(ip);
        }
    }
    
    public void removeAllowedIP(String ip) {
        allowedIPs.remove(ip);
    }
    
    public void addReadUser(String user) {
        readUsers.add(user);
    }
    
    public void removeReadUser(String user) {
        readUsers.remove(user);
    }
    
    public void addWriteUser(String user) {
        writeUsers.add(user);
    }
    
    public void removeWriteUser(String user) {
        writeUsers.remove(user);
    }
    
    public void addAllowedOrigin(String origin) {
        if (!allowedOrigins.contains(origin)) {
            allowedOrigins.add(origin);
        }
    }
    
    public void addAllowedMethod(String method) {
        if (!allowedMethods.contains(method)) {
            allowedMethods.add(method);
        }
    }
    
    // Validation method
    public boolean isValid() {
        // Basic validation rules
        if (bucket == null || bucket.trim().isEmpty()) {
            return false;
        }
        
        // Validate expiration days
        if (objectExpirationDays < 0) {
            return false;
        }
        
        // Validate encryption settings
        if (encryptionEnabled && !encryptionType.equals("AES256") && !encryptionType.equals("none")) {
            return false;
        }
        
        // Validate website hosting settings
        if (websiteEnabled && (indexDocument == null || indexDocument.trim().isEmpty())) {
            return false;
        }
        
        // Validate logging settings
        if (loggingEnabled && (logBucket == null || logBucket.trim().isEmpty())) {
            return false;
        }
        
        return true;
    }
    
    // Check if a specific IP is allowed
    public boolean isIPAllowed(String ip) {
        return publicRead || allowedIPs.isEmpty() || allowedIPs.contains(ip);
    }
    
    // Check if a specific user has read access
    public boolean hasReadAccess(String user) {
        return publicRead || readUsers.contains(user);
    }
    
    // Check if a specific user has write access
    public boolean hasWriteAccess(String user) {
        return publicWrite || writeUsers.contains(user);
    }
}