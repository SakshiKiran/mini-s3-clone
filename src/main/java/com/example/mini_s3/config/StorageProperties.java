package com.example.mini_s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;
import java.nio.file.Path;
import java.nio.file.Paths;


@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {
    private Path root = Paths.get("./storage");
}
