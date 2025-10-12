package com.skndan.veda.resource;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;

import io.smallrye.common.annotation.Blocking;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skndan.veda.config.TenantContext;
import com.skndan.veda.entity.FileInfo;
import com.skndan.veda.entity.FileInfo.IngestionStatus;
import com.skndan.veda.entity.Workspace;
import com.skndan.veda.repo.FileInfoRepo;
import com.skndan.veda.repo.WorkspaceRepo;
import com.skndan.veda.service.DocumentIngestionService;
import com.skndan.veda.service.DocumentIngestionService.IngestionEvent;
import com.skndan.veda.service.MinioService;

import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/v1/file")
public class FileResource {

    @Inject
    MinioService minioService;

    @Inject
    FileInfoRepo fileInfoRepo;

    @Inject
    WorkspaceRepo workspaceRepo;

    @Inject
    DocumentIngestionService ingestionService;

    @Inject
    TenantContext tenantContext;

    @ConfigProperty(name = "quarkus.minio.bucket", defaultValue = "saas-files")
    String bucket;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    JsonWebToken jwt;

    @Inject
    EntityManager entityManager;

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

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    @Transactional
    public Response uploadFile(
            @FormParam("file") List<FileUpload> uploadedFiles,
            @FormParam("workspaceId") String workspaceId,
            @FormParam("autoIngest") String autoIngest) {
        try {

            // Fetch workspace
            Workspace workspace = workspaceRepo.findById(Long.valueOf(workspaceId));
            if (workspace == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Workspace not found"))
                        .build();
            }

            // Get uploader ID from JWT
            String uid = jwt.getSubject();

            // Parse uploader ID
            UUID uploaderId = UUID.fromString(uid);

            // Get tenant ID from context
            String tenantId = tenantContext.getTenantId();

            List<FileUploadResult> results = new ArrayList<>();
            boolean shouldAutoIngest = "true".equalsIgnoreCase(autoIngest);

            System.out.println("Auto Ingest: " + shouldAutoIngest);

            // Process each uploaded file
            for (FileUpload uploadedFile : uploadedFiles) {
                String fileName = uploadedFile.fileName();
                long fileSize = uploadedFile.size();

                System.out.println("Processing file: " + fileName + " of size: " + fileSize);

                // Upload to MinIO
                String objectName = minioService.uploadFile(bucket, fileName, uploadedFile.uploadedFile());

                // Generate storage URL (you can customize this based on your MinIO setup)
                String storageUrl = bucket + "/" + objectName;

                // Extract file type from filename
                String fileType = extractFileType(fileName);

                // Create and persist FileInfo entity
                FileInfo fileInfo = new FileInfo();
                fileInfo.name = fileName;
                fileInfo.type = fileType;
                fileInfo.workspace = workspace;
                fileInfo.uploaderId = uploaderId;
                fileInfo.storageUrl = storageUrl;
                fileInfo.sizeInBytes = BigDecimal.valueOf(fileSize);
                fileInfo.ingestionStatus = IngestionStatus.PENDING;

                fileInfoRepo.persist(fileInfo);
                
                // Flush to ensure the entity is persisted and ID is generated
                entityManager.flush();
                
                System.out.println("FileInfo persisted with ID: " + fileInfo.getId());

                // Trigger async ingestion if autoIngest is enabled and file type is supported
                if (shouldAutoIngest) {
                    ingestionService.triggerIngestion(fileInfo.getId(), storageUrl, tenantId);
                }

                results.add(new FileUploadResult(
                        fileInfo.getId(),
                        fileName,
                        fileType,
                        fileSize,
                        storageUrl));
            }

            String message = shouldAutoIngest
                ? "Files uploaded successfully and ingestion started"
                : "Files uploaded successfully";

            return Response.ok()
                    .entity(new MultipleUploadResponse(results, message))
                    .build();

        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Invalid uploader ID format: " + e.getMessage()))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to upload files: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * SSE endpoint for ingestion status updates filtered by workspace
     * with keepalive heartbeat to prevent connection timeout
     */
    @GET
    @Path("/ingestion-status/{workspaceId}")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<IngestionEvent> streamIngestionStatus(
            @jakarta.ws.rs.PathParam("workspaceId") Long workspaceId) {
        
        // Create heartbeat event to keep connection alive
        IngestionEvent heartbeat = new IngestionEvent(
            null, workspaceId, "heartbeat", IngestionStatus.PROCESSING, "keepalive", 0);
        
        // Main event stream filtered by workspace
        Multi<IngestionEvent> eventStream = ingestionService.getIngestionEventStream()
            .filter(event -> event.workspaceId() != null && event.workspaceId().equals(workspaceId));
        
        // Heartbeat stream - emit every 15 seconds
        Multi<IngestionEvent> heartbeatStream = Multi.createFrom().ticks().every(Duration.ofSeconds(15))
            .map(tick -> heartbeat);
        
        // Merge event stream with heartbeat to prevent timeout
        return Multi.createBy().merging().streams(eventStream, heartbeatStream);
    }

    /**
     * Trigger document ingestion for a specific file
     */
    @POST
    @Path("/ingest/{fileId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    @Transactional
    public Response triggerIngestion(@jakarta.ws.rs.PathParam("fileId") Long fileId) {
        try {
            FileInfo fileInfo = fileInfoRepo.findById(fileId);
            if (fileInfo == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("File not found"))
                        .build();
            }

            if (fileInfo.ingestionStatus == IngestionStatus.COMPLETED) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("File already ingested"))
                        .build();
            }

            // Get tenant ID from context
            String tenantId = tenantContext.getTenantId();

            // Trigger async ingestion
            ingestionService.triggerIngestion(fileId, fileInfo.storageUrl, tenantId);

            return Response.ok()
                    .entity(new IngestionTriggerResponse(fileId, "Ingestion started"))
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Failed to trigger ingestion: " + e.getMessage()))
                    .build();
        }
    }

    /**
     * Extract file type from filename
     */
    private String extractFileType(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * Record to represent the upload response
     */
    public record UploadResponse(String filename, long size, String message) {
    }

    /**
     * Record to represent individual file upload result
     */
    public record FileUploadResult(Long id, String filename, String type, long size, String storageUrl) {
    }

    /**
     * Record to represent multiple file upload response
     */
    public record MultipleUploadResponse(List<FileUploadResult> files, String message) {
    }

    /**
     * Record to represent ingestion trigger response
     */
    public record IngestionTriggerResponse(Long fileId, String message) {
    }

    /**
     * Record to represent error response
     */
    public record ErrorResponse(String error) {
    }
}
