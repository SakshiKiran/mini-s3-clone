package com.example.mini_s3.service;

import com.example.mini_s3.config.StorageProperties;
import com.example.mini_s3.model.ObjectMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class StorageService {
    private final StorageProperties props;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    public void createBucket(String bucket) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        Files.createDirectories(bucketPath);
    }

    public List<String> listBuckets() throws IOException {
        if (!Files.exists(props.getRoot())) return List.of();
        try (Stream<Path> s = Files.list(props.getRoot())) {
            return s.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .toList();
        }
    }

    public ObjectMetadata putObject(String bucket, String key, MultipartFile file) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        if (!Files.exists(bucketPath)) throw new NoSuchFileException("Bucket not found: " + bucket);

        String versionId = UUID.randomUUID().toString();
        String safeName = key.replace("/", "__");
        Path stored = bucketPath.resolve(safeName + "." + versionId + ".bin");
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, stored, StandardCopyOption.REPLACE_EXISTING);
        }

        ObjectMetadata meta = new ObjectMetadata();
        meta.setBucket(bucket);
        meta.setKey(key);
        meta.setSize(file.getSize());
        meta.setContentType(file.getContentType());
        meta.setUploadedAt(Instant.now());
        meta.setVersionId(versionId);

        Path metaFile = bucketPath.resolve(safeName + "." + versionId + ".meta.json");
        mapper.writeValue(metaFile.toFile(), meta);
        return meta;
    }

    public List<ObjectMetadata> listObjects(String bucket) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        if (!Files.exists(bucketPath)) throw new NoSuchFileException("Bucket not found: " + bucket);
        try (Stream<Path> s = Files.list(bucketPath)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".meta.json"))
                    .map(p -> {
                        try {
                            return mapper.readValue(p.toFile(), ObjectMetadata.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    public ResponseEntity<Resource> getObject(String bucket, String key) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        if (!Files.exists(bucketPath)) throw new NoSuchFileException("Bucket not found: " + bucket);
        String prefix = key.replace("/", "__") + ".";
        try (Stream<Path> s = Files.list(bucketPath)) {
            Optional<Path> latest = s.filter(p -> p.getFileName().toString().startsWith(prefix) 
                    && p.getFileName().toString().endsWith(".bin"))
                    .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
            if (latest.isEmpty()) throw new NoSuchFileException("Object not found: " + key);
            Path filePath = latest.get();
            ByteArrayResource res = new ByteArrayResource(Files.readAllBytes(filePath));
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" 
                        + Paths.get(key).getFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(res);
        }
    }

    public void deleteObject(String bucket, String key) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        String prefix = key.replace("/", "__") + ".";
        try (Stream<Path> s = Files.list(bucketPath)) {
            s.filter(p -> p.getFileName().toString().startsWith(prefix))
             .forEach(p -> {
                 try { Files.deleteIfExists(p); } catch (IOException ignored) {}
             });
        }
    }
}
