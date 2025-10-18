package com.skndan.veda.service;

import com.skndan.veda.config.TenantContext;
import com.skndan.veda.entity.FileInfo;
import com.skndan.veda.entity.FileInfo.IngestionStatus;
import com.skndan.veda.repo.FileInfoRepo;
import com.skndan.veda.service.DocumentTextExtractor.PageContent;

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
import java.util.List;

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
        public void ingestDocument(JsonObject message) {
                System.out.println("ConsumeEvent triggered with: " + message.encode());

                Long fileInfoId = message.getLong("fileInfoId");
                String filePath = message.getString("filePath");
                String tenantId = message.getString("tenantId");

                System.out.printf("Starting ingestion for FileInfo ID: %d, filePath: %s, tenantId: %s%n",
                                fileInfoId, filePath, tenantId);

                FileInfo fileInfo = fileInfoRepo.findById(fileInfoId);
                if (fileInfo == null) {
                        publishEvent(new IngestionEvent(fileInfoId, null, null,
                                        IngestionStatus.FAILED, "FileInfo not found", null));
                        return;
                }

                // Update to PROCESSING inside a short transaction
                updateFileStatus(fileInfoId, IngestionStatus.PROCESSING, null);

                try {
                        publishEvent(new IngestionEvent(fileInfo.getId(), fileInfo.workspace.getId(),
                                        fileInfo.name, IngestionStatus.PROCESSING, "Downloading file...", 10));

                        File file = minioService.downloadFile(fileInfo.storageUrl);

                        publishEvent(new IngestionEvent(fileInfo.getId(), fileInfo.workspace.getId(),
                                        fileInfo.name, IngestionStatus.PROCESSING, "File downloaded", 30));

                        if (!textExtractor.isSupported(fileInfo.type))
                                throw new UnsupportedOperationException("Unsupported file type: " + fileInfo.type);

                        // String text = textExtractor.extractText(file, fileInfo.type);

                        List<PageContent> pc = textExtractor.extractPerPage(file, fileInfo.name);

                        publishEvent(new IngestionEvent(fileInfo.getId(), fileInfo.workspace.getId(),
                                        fileInfo.name, IngestionStatus.PROCESSING, "Text extracted", 60));

                        // **Embedding and Qdrant ingestion – long running with progress updates**
                        qdrantService.ingest(pc, tenantId,
                                        fileInfo.workspace.getId().toString(),
                                        fileInfo.uploaderId.toString(),
                                        fileInfo.name,
                                        progressInfo -> {
                                                // Calculate progress between 60% and 90%
                                                int embeddingProgress = 60
                                                                + (progressInfo.getProgressPercentage() * 30 / 100);
                                                publishEvent(new IngestionEvent(
                                                                fileInfo.getId(),
                                                                fileInfo.workspace.getId(),
                                                                fileInfo.name,
                                                                IngestionStatus.PROCESSING,
                                                                progressInfo.message(),
                                                                embeddingProgress));
                                        });

                        publishEvent(new IngestionEvent(fileInfo.getId(), fileInfo.workspace.getId(),
                                        fileInfo.name, IngestionStatus.PROCESSING, "Vector embedding completed", 90));

                        file.delete();

                        updateFileStatus(fileInfoId, IngestionStatus.COMPLETED, null);

                        publishEvent(new IngestionEvent(fileInfo.getId(), fileInfo.workspace.getId(),
                                        fileInfo.name, IngestionStatus.COMPLETED, "Document ingestion completed", 100));

                } catch (Exception e) {
                        updateFileStatus(fileInfoId, IngestionStatus.FAILED, e.getMessage());

                        publishEvent(new IngestionEvent(fileInfo.getId(), fileInfo.workspace.getId(),
                                        fileInfo.name, IngestionStatus.FAILED,
                                        "Ingestion failed: " + e.getMessage(), 0));

                        e.printStackTrace();
                }
        }

        @Transactional
        void updateFileStatus(Long fileInfoId, IngestionStatus status, String error) {
                FileInfo fileInfo = fileInfoRepo.findById(fileInfoId);
                if (fileInfo == null)
                        return;
                fileInfo.ingestionStatus = status;
                fileInfo.ingestionError = error;
                fileInfoRepo.persist(fileInfo);
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
        public record IngestionRequest(Long fileInfoId, String filePath, String tenantId) {
        }

        /**
         * Event record for SSE streaming with workspace and file details
         */
        public record IngestionEvent(
                        Long fileId,
                        Long workspaceId,
                        String fileName,
                        IngestionStatus status,
                        String message,
                        Integer progress) {
        }
}