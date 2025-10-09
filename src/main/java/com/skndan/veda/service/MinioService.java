package com.skndan.veda.service;

import com.skndan.veda.config.TenantContext;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
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
}
