package com.skndan.veda.resource;

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

    @GET
    @Path("/upload-url")
    @Produces(MediaType.APPLICATION_JSON)
    public UploadUrlResponse getUploadUrl(@QueryParam("filename") String filename) {
        String url = minioService.generateUploadUrl("saas-files", filename);
        return new UploadUrlResponse(url);
    }

    /**
     * Record to represent the upload URL response
     */
    public record UploadUrlResponse(String url) {}
}
