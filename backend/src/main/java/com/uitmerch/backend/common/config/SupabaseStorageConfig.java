package com.uitmerch.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.net.URI;
import org.springframework.context.annotation.Profile;

/**
 * AWS S3 / Supabase Storage configuration.
 * Inactive in dev and docker profiles — DevStorageService handles storage there.
 * NFR03: Supabase Storage used for images; Base64/BLOB persistence in DB prohibited.
 */
@Configuration
@Profile("!(dev | docker)")
public class SupabaseStorageConfig {
    
    @Value("${app.storage.endpoint}")
    private String endpoint;
    
    @Value("${app.storage.region}")
    private String region;
    
    @Value("${app.storage.access-key}")
    private String accessKey;
    
    @Value("${app.storage.secret-key}")
    private String secretKey;
    
    /**
     * Configure S3 client for Supabase Storage.
     * Uses custom endpoint pointing to Supabase Storage API.
     * @return configured S3Client bean
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
            ))
            .endpointOverride(URI.create(endpoint))
            .serviceConfiguration(S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build())
            .build();
    }
}
