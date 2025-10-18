package com.skndan.veda.service;

import com.skndan.veda.config.TenantContext;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MinioService {

  @Inject
  MinioClient minioClient;

  @Inject
  TenantContext tenantContext;

  public String generateUploadUrl(String bucket, String filename) {
    try {

      String path = tenantContext.getTenantId() + "/" + filename;

      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.PUT)
              .bucket(bucket)
              .object(path)
              .expiry(60 * 5) // 5 minutes
              .build());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public String getPreviewUrl(String bucket, String objectPath) throws Exception {
    return minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .method(Method.GET)
            .bucket(bucket)
            .object(objectPath)
            .expiry(1, TimeUnit.HOURS) // 1 hour valid
            .build());
  }

  public String uploadFile(String bucket, String filename, Path filePath) {
    try {
      String objectName = tenantContext.getTenantId() + "/" + filename;

      try (InputStream inputStream = Files.newInputStream(filePath)) {
        long fileSize = Files.size(filePath);
        String contentType = Files.probeContentType(filePath);

        if (contentType == null) {
          contentType = "application/octet-stream";
        }

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(inputStream, fileSize, -1)
                .contentType(contentType)
                .build());
      }

      return objectName;
    } catch (Exception e) {
      throw new RuntimeException("Failed to upload file to MinIO: " + e.getMessage(), e);
    }
  }

  public String uploadBookFile(String bucket, String objectName, Path filePath) {
    try {
      try (InputStream inputStream = Files.newInputStream(filePath)) {
        long fileSize = Files.size(filePath);
        String contentType = Files.probeContentType(filePath);

        if (contentType == null) {
          contentType = "application/octet-stream";
        }

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectName)
                .stream(inputStream, fileSize, -1)
                .contentType(contentType)
                .build());
      }

      return objectName;
    } catch (Exception e) {
      throw new RuntimeException("Failed to upload file to MinIO: " + e.getMessage(), e);
    }
  }

  /**
   * Download a file from MinIO
   *
   * @param storageUrl The storage URL (bucket/objectName format)
   * @return Downloaded file
   */
  public File downloadFile(String storageUrl) {
    try {
      System.out.println("Downloading file from MinIO: " + storageUrl);
      // Parse bucket and object name from storage URL
      String[] parts = storageUrl.split("/", 2);
      String bucket = parts[0];
      String objectName = parts[1];

      // Create temporary file
      File tempFile = File.createTempFile("minio-", "-" + objectName.substring(objectName.lastIndexOf("/") + 1));
      tempFile.deleteOnExit();

      // Download from MinIO
      try (InputStream stream = minioClient.getObject(
          GetObjectArgs.builder()
              .bucket(bucket)
              .object(objectName)
              .build());
          FileOutputStream fos = new FileOutputStream(tempFile)) {

        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = stream.read(buffer)) != -1) {
          fos.write(buffer, 0, bytesRead);
        }
      }

      return tempFile;
    } catch (Exception e) {
      throw new RuntimeException("Failed to download file from MinIO: " + e.getMessage(), e);
    }
  }
}
