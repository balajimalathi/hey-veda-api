package com.skndan.veda.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
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
         * Ingest document with multi-tenancy metadata
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
                System.out.println("Starting ingestion into Qdrant for file: " + fileName + ", tenantId: " + tenantId
                                + ", workspaceId: " + workspaceId + ", userId: " + userId);
                // Create comprehensive metadata for multi-tenancy
                Map<String, String> meta = createMetadata(
                                tenantId,
                                workspaceId,
                                userId,
                                fileName);

                Document document = Document.document(text, new Metadata(meta));

                EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                                .embeddingStore(store)
                                .embeddingModel(embeddingModel)
                                .documentSplitter(recursive(768, 0))
                                .build();

                ingestor.ingest(document);
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
}
