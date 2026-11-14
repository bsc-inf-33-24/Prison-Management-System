package com.pms.api.inmate.service;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioStorageConfiguration {
    @Bean
    MinioClient minioClient(@Value("${app.storage.minio.endpoint}") String endpoint,
                            @Value("${app.storage.minio.access-key}") String accessKey,
                            @Value("${app.storage.minio.secret-key}") String secretKey) {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }
}
