package com.skndan.veda.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

@ApplicationScoped
public class QdrantService {

        @Inject
        QdrantEmbeddingStore store;

        @Inject
        EmbeddingModel embeddingModel;

        /**
         * Ingest document with multi-tenancy metadata and progress callback
         *
         * @param text             Document text content
         * @param tenantId         Tenant identifier for multi-tenancy
         * @param workspaceId      Workspace identifier
         * @param userId           User who uploaded the document
         * @param fileName         Original file name
         * @param progressCallback Callback for progress updates (segment index, total segments)
         */
        public void ingest(
                        String text,
                        String tenantId,
                        String workspaceId,
                        String userId,
                        String fileName,
                        Consumer<ProgressInfo> progressCallback) {
                System.out.println("Starting ingestion into Qdrant for file: " + fileName + ", tenantId: " + tenantId
                                + ", workspaceId: " + workspaceId + ", userId: " + userId);
                
                // Create comprehensive metadata for multi-tenancy
                Map<String, String> meta = createMetadata(
                                tenantId,
                                workspaceId,
                                userId,
                                fileName);

                Document document = Document.document(text, new Metadata(meta));

                // Split document into segments
                var splitter = recursive(768, 0);
                List<TextSegment> segments = splitter.split(document);
                
                int totalSegments = segments.size();
                System.out.println("Document split into " + totalSegments + " segments");
                
                // Notify about splitting completion
                if (progressCallback != null) {
                        progressCallback.accept(new ProgressInfo(0, totalSegments, "Document split into segments"));
                }

                // Process each segment individually with progress updates
                for (int i = 0; i < segments.size(); i++) {
                        TextSegment segment = segments.get(i);
                        
                        // Generate embedding for this segment
                        Embedding embedding = embeddingModel.embed(segment).content();
                        
                        // Store in Qdrant
                        store.add(embedding, segment);
                        
                        // Report progress
                        if (progressCallback != null) {
                                int currentSegment = i + 1;
                                String message = String.format("Processed segment %d/%d", currentSegment, totalSegments);
                                progressCallback.accept(new ProgressInfo(currentSegment, totalSegments, message));
                        }
                        
                        System.out.println("Processed segment " + (i + 1) + "/" + totalSegments);
                }
                
                System.out.println("Completed ingestion of " + totalSegments + " segments");
        }

        /**
         * Ingest document with multi-tenancy metadata (without progress callback)
         *
         * @param text        Document text content
         * @param tenantId    Tenant identifier for multi-tenancy
         * @param workspaceId Workspace identifier
         * @param userId      User who uploaded the document
         * @param fileName    Original file name
         */
        public void ingest(
                        String text,
                        String tenantId,
                        String workspaceId,
                        String userId,
                        String fileName) {
                ingest(text, tenantId, workspaceId, userId, fileName, null);
        }

        @PreDestroy
        void close() {
                store.close(); // closes ManagedChannel inside
        }

        /**
         * Upsert a document with multi-tenancy metadata
         * 
         * @param id          Document unique identifier
         * @param text        Document text content
         * @param tenantId    Tenant identifier
         * @param workspaceId Workspace identifier
         * @param userId      User identifier
         * @param fileName    File name
         */
        public void upsert(
                        String id,
                        String text,
                        String tenantId,
                        String workspaceId,
                        String userId,
                        String fileName) {
                Embedding embedding = embeddingModel.embed(text).content();

                // Create comprehensive metadata
                Map<String, String> metadataMap = createMetadata(
                                tenantId,
                                workspaceId,
                                userId,
                                fileName);

                // Add document ID to metadata
                metadataMap.put("documentId", id);

                System.out.println("Embedding vector size: " + embedding.vector().length);

                Metadata data = new Metadata(metadataMap);
                TextSegment textSegment = TextSegment.from(text, data);
                store.add(embedding, textSegment);
        }

        /**
         * Create string-based metadata map
         */
        private Map<String, String> createMetadata(
                        String tenantId,
                        String workspaceId,
                        String userId,
                        String fileName) {
                Map<String, String> meta = new HashMap<>();
                meta.put("tenantId", tenantId);
                meta.put("workspaceId", workspaceId);
                meta.put("userId", userId);
                meta.put("fileName", fileName);
                meta.put("uploadedAt", Instant.now().toString());
                return meta;
        }

        /**
         * Record to hold progress information during ingestion
         */
        public record ProgressInfo(
                int currentSegment,
                int totalSegments,
                String message
        ) {
                public int getProgressPercentage() {
                        if (totalSegments == 0) return 0;
                        return (int) ((currentSegment * 100.0) / totalSegments);
                }
        }
}
