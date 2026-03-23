package com.wms.service;

import com.wms.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class StorageService {

    private static final Logger log = LogManager.getLogger(StorageService.class);

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Value("${app.storage.max-file-size:10485760}")
    private long maxFileSize;

    public StorageService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    public String store(MultipartFile file, Long orderId) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store an empty file");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed size of " + (maxFileSize / 1024 / 1024) + "MB");
        }

        String extension = getExtension(file.getOriginalFilename());
        String objectKey = buildObjectKey(orderId, UUID.randomUUID().toString(), extension);
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build());

            log.info("Uploaded '{}' to MinIO as '{}' for order {}", file.getOriginalFilename(), objectKey, orderId);
            return objectKey;

        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to MinIO: " + e.getMessage(), e);
        }
    }

    public InputStream download(String objectKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to download file from MinIO: " + e.getMessage(), e);
        }
    }

    public String generatePresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate pre-signed URL: " + e.getMessage(), e);
        }
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(minioProperties.getBucket())
                    .object(objectKey)
                    .build());
            log.info("Deleted object '{}' from MinIO", objectKey);
        } catch (Exception e) {
            log.warn("Could not delete object '{}' from MinIO: {}", objectKey, e.getMessage());
        }
    }

    private String buildObjectKey(Long orderId, String uuid, String extension) {
        return "order-" + orderId + "/" + uuid + (extension.isEmpty() ? "" : "." + extension);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
