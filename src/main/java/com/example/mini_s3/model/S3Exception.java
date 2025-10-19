package com.example.mini_s3.model;

public class S3Exception extends RuntimeException {
    private final String code; // Like NoSuchBucket, NoSuchKey, etc.
    private final String resource;
    
    public S3Exception(String code, String message, String resource) {
        super(message);
        this.code = code;
        this.resource = resource;
    }

    public String getCode() {
        return code;
    }

    public String getResource() {
        return resource;
    }
}