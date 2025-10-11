package com.skndan.veda.service;

import com.skndan.veda.config.TenantContext;
import com.skndan.veda.entity.FileInfo;
import com.skndan.veda.entity.FileInfo.IngestionStatus;
import com.skndan.veda.repo.FileInfoRepo;
import io.quarkus.vertx.ConsumeEvent;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.File;

/**
 * Service for asynchronously ingesting documents into Qdrant
 * with SSE progress updates
 */
@ApplicationScoped
public class DocumentIngestionService {

    @Inject
    EventBus eventBus;

    @Inject
    FileInfoRepo fileInfoRepo;

    @Inject
    QdrantService qdrantService;

    @Inject
    DocumentTextExtractor textExtractor;

    @Inject
    MinioService minioService;

    @Inject
    TenantContext tenantContext;

    // SSE broadcaster for ingestion events
    private final BroadcastProcessor<IngestionEvent> ingestionProcessor = BroadcastProcessor.create();

    /**
     * Trigger async document ingestion
     * 
     * @param fileInfoId ID of the FileInfo entity
     * @param filePath   Path to the uploaded file
     * @param tenantId   Tenant ID for multi-tenancy
     */
    public void triggerIngestion(Long fileInfoId, String filePath, String tenantId) {
        System.out.println("Triggering ingestion for FileInfo ID: " + fileInfoId);
        JsonObject message = new JsonObject()
            .put("fileInfoId", fileInfoId)
            .put("filePath", filePath)
            .put("tenantId", tenantId);
        eventBus.send("document.ingest", message);
        System.out.println("Event sent to document.ingest: " + message.encode());
    }

    /**
     * Handle async document ingestion
     */
    @ConsumeEvent("document.ingest")
    @Blocking
    @Transactional
    public void ingestDocument(JsonObject message) {
        System.out.println("ConsumeEvent triggered with: " + message.encode());
        
        Long fileInfoId = message.getLong("fileInfoId");
        String filePath = message.getString("filePath");
        String tenantId = message.getString("tenantId");
        
        System.out.println("Starting ingestion for FileInfo ID: " + fileInfoId + ", filePath: " + filePath + ", tenantId: " + tenantId);

        FileInfo fileInfo = null;
        try {
            // Update status to PROCESSING
            fileInfo = fileInfoRepo.findById(fileInfoId);

            if (fileInfo == null) {
                System.out.println("FileInfo not found for ID: " + fileInfoId);
                publishEvent(new IngestionEvent(
                    fileInfoId,
                    null,
                    null,
                    IngestionStatus.FAILED,
                    "FileInfo not found",
                    null
                ));
                return;
            }

            fileInfo.ingestionStatus = IngestionStatus.PROCESSING;
            fileInfoRepo.persist(fileInfo);
            
            publishEvent(new IngestionEvent(
                fileInfo.getId(),
                fileInfo.workspace.getId(),
                fileInfo.name,
                IngestionStatus.PROCESSING,
                "Starting document ingestion...",
                10
            ));

            // Download file from MinIO
            File file = minioService.downloadFile(fileInfo.storageUrl);
            publishEvent(new IngestionEvent(
                fileInfo.getId(),
                fileInfo.workspace.getId(),
                fileInfo.name,
                IngestionStatus.PROCESSING,
                "File downloaded from storage",
                30
            ));

            // Check if file type is supported
            if (!textExtractor.isSupported(fileInfo.type)) {
                throw new UnsupportedOperationException(
                    "Unsupported file type: " + fileInfo.type);
            }

            // Extract text from document
            String text = textExtractor.extractText(file, fileInfo.type);
            System.out.println("Extracted text length: " + (text != null ? text.length() : 0));
            publishEvent(new IngestionEvent(
                fileInfo.getId(),
                fileInfo.workspace.getId(),
                fileInfo.name,
                IngestionStatus.PROCESSING,
                "Text extracted from document",
                60
            ));

            // Ingest into Qdrant
            qdrantService.ingest(
                text,
                tenantId,
                fileInfo.workspace.getId().toString(),
                fileInfo.uploaderId.toString(),
                fileInfo.name
            );
            publishEvent(new IngestionEvent(
                fileInfo.getId(),
                fileInfo.workspace.getId(),
                fileInfo.name,
                IngestionStatus.PROCESSING,
                "Document ingested into vector store",
                90
            ));

            // Clean up temporary file
            file.delete();

            // Update status to COMPLETED
            fileInfo.ingestionStatus = IngestionStatus.COMPLETED;
            fileInfo.ingestionError = null;
            fileInfoRepo.persist(fileInfo);

            publishEvent(new IngestionEvent(
                fileInfo.getId(),
                fileInfo.workspace.getId(),
                fileInfo.name,
                IngestionStatus.COMPLETED,
                "Document ingestion completed successfully",
                100
            ));

        } catch (Exception e) {
            // Update status to FAILED
            if (fileInfo != null) {
                fileInfo.ingestionStatus = IngestionStatus.FAILED;
                fileInfo.ingestionError = e.getMessage();
                fileInfoRepo.persist(fileInfo);

                publishEvent(new IngestionEvent(
                    fileInfo.getId(),
                    fileInfo.workspace.getId(),
                    fileInfo.name,
                    IngestionStatus.FAILED,
                    "Ingestion failed: " + e.getMessage(),
                    0
                ));
            } else {
                publishEvent(new IngestionEvent(
                    fileInfoId,
                    null,
                    null,
                    IngestionStatus.FAILED,
                    "Ingestion failed: " + e.getMessage(),
                    0
                ));
            }
            
            e.printStackTrace();
        }
    }

    /**
     * Get SSE stream for ingestion events
     */
    public Multi<IngestionEvent> getIngestionEventStream() {
        return ingestionProcessor
            .onOverflow().drop();
    }

    /**
     * Publish ingestion event to SSE stream
     */
    private void publishEvent(IngestionEvent event) {
        ingestionProcessor.onNext(event);
    }

    /**
     * Request record for async ingestion
     */
    public record IngestionRequest(Long fileInfoId, String filePath, String tenantId) {}

    /**
     * Event record for SSE streaming with workspace and file details
     */
    public record IngestionEvent(
        Long fileId,
        Long workspaceId,
        String fileName,
        IngestionStatus status,
        String message,
        Integer progress
    ) {}
}