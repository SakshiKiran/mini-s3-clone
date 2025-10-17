package com.example.mini_s3.controller;

import com.example.mini_s3.model.ObjectMetadata;
import com.example.mini_s3.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/buckets")
@RequiredArgsConstructor
public class BucketController {
    private final StorageService storage;

    @PostMapping("/{bucket}")
    public ResponseEntity<String> createBucket(@PathVariable String bucket) throws IOException {
        storage.createBucket(bucket);
        return ResponseEntity.status(HttpStatus.CREATED).body("Bucket created: " + bucket);
    }

    @GetMapping
    public List<String> listBuckets() throws IOException {
        return storage.listBuckets();
    }

    
}

/*
    @GetMapping("/{bucket}/objects")
    public List<ObjectMetadata> listObjects(@PathVariable String bucket) throws IOException {
        return storage.listObjects(bucket);
    } */
