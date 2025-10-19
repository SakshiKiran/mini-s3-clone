package com.example.mini_s3.service;

import com.example.mini_s3.config.StorageProperties;
import com.example.mini_s3.model.ObjectMetadata;
import com.example.mini_s3.model.BucketPolicy;
import com.example.mini_s3.model.ListObjectsResult;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.io.File;
import java.io.OutputStream;
@Service
@RequiredArgsConstructor
public class StorageService {
    private final StorageProperties props;
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final Path root = Paths.get("storage");

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

    public void deleteBucket(String bucket) throws IOException {
        Path bucketPath = root.resolve(bucket);
        if (Files.exists(bucketPath)) {
            Files.walk(bucketPath)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    public List<ObjectMetadata> listObjectVersions(String bucket, String key) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        if (!Files.exists(bucketPath)) {
            throw new NoSuchFileException("Bucket not found: " + bucket);
        }

        String prefix = key.replace("/", "__") + ".";
        try (Stream<Path> s = Files.list(bucketPath)) {
            return s.filter(p -> p.getFileName().toString().startsWith(prefix) 
                    && p.getFileName().toString().endsWith(".meta.json"))
                    .map(p -> {
                        try {
                            return mapper.readValue(p.toFile(), ObjectMetadata.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(ObjectMetadata::getUploadedAt).reversed())
                    .collect(Collectors.toList());
        }
    }

    public void enableVersioning(String bucket) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        if (!Files.exists(bucketPath)) {
            throw new NoSuchFileException("Bucket not found: " + bucket);
        }
        BucketPolicy policy = new BucketPolicy(bucket);
        policy.setVersioningEnabled(true);
        Path policyFile = bucketPath.resolve(".policy.json");
        mapper.writeValue(policyFile.toFile(), policy);
    }

    public void disableVersioning(String bucket) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        if (!Files.exists(bucketPath)) {
            throw new NoSuchFileException("Bucket not found: " + bucket);
        }
        BucketPolicy policy = new BucketPolicy(bucket);
        policy.setVersioningEnabled(false);
        Path policyFile = bucketPath.resolve(".policy.json");
        mapper.writeValue(policyFile.toFile(), policy);
    }

    // Multipart upload support
    public String initiateMultipartUpload(String bucket, String key) {
        return UUID.randomUUID().toString();
    }

    public void uploadPart(String bucket, String key, String uploadId, int partNumber, MultipartFile part) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        if (!Files.exists(bucketPath)) {
            throw new NoSuchFileException("Bucket not found: " + bucket);
        }
        
        String safeName = key.replace("/", "__");
        Path partPath = bucketPath.resolve(String.format("%s.%s.part%d", safeName, uploadId, partNumber));
        try (InputStream in = part.getInputStream()) {
            Files.copy(in, partPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void completeMultipartUpload(String bucket, String key, String uploadId, List<Integer> parts) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        if (!Files.exists(bucketPath)) {
            throw new NoSuchFileException("Bucket not found: " + bucket);
        }

        String safeName = key.replace("/", "__");
        String versionId = UUID.randomUUID().toString();
        Path finalPath = bucketPath.resolve(safeName + "." + versionId + ".bin");
        
        try (OutputStream out = Files.newOutputStream(finalPath)) {
            for (Integer partNumber : parts) {
                Path partPath = bucketPath.resolve(String.format("%s.%s.part%d", safeName, uploadId, partNumber));
                Files.copy(partPath, out);
                Files.delete(partPath);
            }
        }

        ObjectMetadata meta = new ObjectMetadata();
        meta.setBucket(bucket);
        meta.setKey(key);
        meta.setSize(Files.size(finalPath));
        meta.setContentType("application/octet-stream");
        meta.setUploadedAt(Instant.now());
        meta.setVersionId(versionId);

        Path metaFile = bucketPath.resolve(safeName + "." + versionId + ".meta.json");
        mapper.writeValue(metaFile.toFile(), meta);
    }
    public ListObjectsResult listObjects(String bucket, String prefix, String delimiter, String continuationToken, int maxKeys) throws IOException {
        Path bucketPath = props.getRoot().resolve(bucket);
        if (!Files.exists(bucketPath)) {
            throw new NoSuchFileException("Bucket not found: " + bucket);
        }

        ListObjectsResult result = new ListObjectsResult(bucket, prefix, delimiter, maxKeys);

        try (Stream<Path> s = Files.list(bucketPath)) {
            List<ObjectMetadata> allObjects = s.filter(p -> p.getFileName().toString().endsWith(".meta.json"))
                    .map(p -> {
                        try {
                            return mapper.readValue(p.toFile(), ObjectMetadata.class);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .filter(obj -> prefix == null || obj.getKey().startsWith(prefix))
                    .sorted(Comparator.comparing(ObjectMetadata::getKey))
                    .collect(Collectors.toList());

            // Handle continuation token (pagination)
            if (continuationToken != null) {
                int startIndex = -1;
                for (int i = 0; i < allObjects.size(); i++) {
                    if (allObjects.get(i).getKey().equals(continuationToken)) {
                        startIndex = i + 1;
                        break;
                    }
                }
                if (startIndex >= 0 && startIndex < allObjects.size()) {
                    allObjects = allObjects.subList(startIndex, allObjects.size());
                }
            }

            // Handle delimiter-based listing (folder-like structure)
            if (delimiter != null && !delimiter.isEmpty()) {
                Set<String> prefixes = new HashSet<>();
                List<ObjectMetadata> filteredObjects = new ArrayList<>();

                for (ObjectMetadata obj : allObjects) {
                    String key = obj.getKey();
                    if (!key.startsWith(prefix == null ? "" : prefix)) {
                        continue;
                    }

                    String remainingKey = key.substring(prefix == null ? 0 : prefix.length());
                    int delimiterIndex = remainingKey.indexOf(delimiter);

                    if (delimiterIndex >= 0) {
                        prefixes.add(prefix == null ? remainingKey.substring(0, delimiterIndex + 1) 
                                : prefix + remainingKey.substring(0, delimiterIndex + 1));
                    } else {
                        filteredObjects.add(obj);
                    }
                }

                result.setObjects(filteredObjects);
                result.setCommonPrefixes(new ArrayList<>(prefixes));
            } else {
                result.setObjects(allObjects);
            }

            // Handle pagination
            if (maxKeys > 0 && result.getObjects().size() > maxKeys) {
                result.setTruncated(true);
                String lastKey = result.getObjects().get(maxKeys - 1).getKey();
                result.setNextContinuationToken(lastKey);
                result.setObjects(result.getObjects().subList(0, maxKeys));
            }

            return result;
        }
    }
}