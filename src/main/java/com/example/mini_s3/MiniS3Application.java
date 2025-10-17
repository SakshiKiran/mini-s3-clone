package com.example.mini_s3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.example.mini_s3.config.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class MiniS3Application {
    public static void main(String[] args) {
        SpringApplication.run(MiniS3Application.class, args);
    }
}
