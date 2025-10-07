package com.skndan.veda.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MinioService {

  @Inject
  MinioClient minioClient;

  public String generateUploadUrl(String bucket, String filename) {
    try {
      return minioClient.getPresignedObjectUrl(
          GetPresignedObjectUrlArgs.builder()
              .method(Method.PUT)
              .bucket(bucket)
              .object(filename)
              .expiry(60 * 5) // 5 minutes
              .build());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
