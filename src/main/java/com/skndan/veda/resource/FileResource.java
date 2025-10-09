package com.skndan.veda.resource;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.skndan.veda.service.MinioService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/v1/file")
public class FileResource {

    @Inject
    MinioService minioService;

    @ConfigProperty(name = "quarkus.minio.bucket", defaultValue = "saas-files")
    String bucket;

    /**
     * Record to represent the upload URL response
     */
    public record UploadUrlResponse(String url) {
    }

    @GET
    @Path("/upload-url")
    @Produces(MediaType.APPLICATION_JSON)
    public UploadUrlResponse getUploadUrl(@QueryParam("filename") String filename) {
        String url = minioService.generateUploadUrl(bucket, filename);
        return new UploadUrlResponse(url);
    }

}
