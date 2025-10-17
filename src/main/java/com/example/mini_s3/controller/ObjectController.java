package com.example.mini_s3.controller;

import com.example.mini_s3.model.ObjectMetadata;
import com.example.mini_s3.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/buckets/{bucket}/objects")
@RequiredArgsConstructor
public class ObjectController {
    private final StorageService storage;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ObjectMetadata putObject(
            @PathVariable String bucket,
            @RequestParam("key") String key,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        return storage.putObject(bucket, key, file);
    }

    @GetMapping
    public List<ObjectMetadata> list(@PathVariable String bucket) throws IOException {
        return storage.listObjects(bucket);
    }

    @GetMapping("/{key}")
    public ResponseEntity<Resource> get(
            @PathVariable String bucket,
            @PathVariable String key
    ) throws IOException {
        return storage.getObject(bucket, key);
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<Void> delete(
            @PathVariable String bucket,
            @PathVariable String key
    ) throws IOException {
        storage.deleteObject(bucket, key);
        return ResponseEntity.noContent().build();
    }
}
