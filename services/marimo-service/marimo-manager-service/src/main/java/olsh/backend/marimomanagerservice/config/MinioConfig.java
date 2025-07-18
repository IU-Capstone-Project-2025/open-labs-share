package olsh.backend.marimomanagerservice.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Configuration
@Slf4j
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public String getBucketName() {
        return bucketName;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            // Create a separate MinioClient instance for initialization to avoid circular dependency
            MinioClient initClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
                    
            boolean found = initClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                initClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' created successfully.", bucketName);
            } else {
                log.info("MinIO bucket '{}' already exists.", bucketName);
            }
            setPublicReadPolicy(initClient, bucketName);

        } catch (MinioException | IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error while initializing MinIO bucket '{}'", bucketName, e);
            throw new RuntimeException("Could not initialize MinIO bucket", e);
        }
    }

    private void setPublicReadPolicy(MinioClient minioClient, String bucketName) {
        try {
            String policyJson = """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Principal": "*",
                            "Action": [
                                "s3:GetObject",
                                "s3:ListBucket"
                            ],
                            "Resource": [
                                "arn:aws:s3:::%s/*",
                                "arn:aws:s3:::%s"
                            ]
                        }
                    ]
                }
                """.formatted(bucketName, bucketName);

            minioClient.setBucketPolicy(
                SetBucketPolicyArgs.builder()
                    .bucket(bucketName)
                    .config(policyJson)
                    .build()
            );
            log.info("Successfully set public read policy on bucket '{}'", bucketName);
        } catch (Exception e) {
            log.error("Error setting public read policy on bucket '{}'", bucketName, e);
            throw new RuntimeException("Error setting public read policy on bucket " + bucketName, e);
        }
    }
}