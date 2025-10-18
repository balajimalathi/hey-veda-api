package com.skndan.veda.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.skndan.veda.config.RequestLoggingFilter;
import com.skndan.veda.config.TenantContext;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

@ApplicationScoped
public class QdrantRetriever implements Supplier<RetrievalAugmentor> {
    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class.getName());

    @Inject
    Provider<TenantContext> tenantContextProvider;

    private final QdrantEmbeddingStore store;
    private final EmbeddingModel model;

    // Thread-local storage for retrieved sources
    private static final ThreadLocal<List<SourceInfo>> RETRIEVED_SOURCES = ThreadLocal.withInitial(ArrayList::new);

    @Inject
    QdrantRetriever(QdrantEmbeddingStore store, EmbeddingModel model) {
        this.store = store;
        this.model = model;
    }

    public static List<SourceInfo> getLastRetrievedSources() {
        return new ArrayList<>(RETRIEVED_SOURCES.get());
    }

    public static void clearSources() {
        RETRIEVED_SOURCES.remove();
    }

    @Override
    public RetrievalAugmentor get() {
        String tenantId = tenantContextProvider.get().getTenantId();
        LOG.info(tenantId + " - Augmenting with tenant-aware retriever");

        EmbeddingStoreContentRetriever baseRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .maxResults(10)
                .minScore(0.7)
                .filter(MetadataFilterBuilder.metadataKey("tenantId").isEqualTo(tenantId))
                .build();

        // Wrap retriever to capture sources
        SourceCapturingRetriever capturingRetriever = new SourceCapturingRetriever(baseRetriever, RETRIEVED_SOURCES);

        // Return augmentor with source-capturing retriever
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(capturingRetriever)
                .build();
    }

    // Wrapper ContentRetriever that captures sources
    private static class SourceCapturingRetriever implements ContentRetriever {
        private final ContentRetriever delegate;
        private final ThreadLocal<List<SourceInfo>> sourcesStorage;

        SourceCapturingRetriever(ContentRetriever delegate, ThreadLocal<List<SourceInfo>> sourcesStorage) {
            this.delegate = delegate;
            this.sourcesStorage = sourcesStorage;
        }

        @Override
        public List<Content> retrieve(Query query) {
            // Clear previous sources
            sourcesStorage.get().clear();

            // Delegate retrieval
            List<Content> contents = delegate.retrieve(query);

            if (contents != null && !contents.isEmpty()) {
                // Capture sources from retrieved content
                for (int i = 0; i < contents.size(); i++) {
                    Content content = contents.get(i);
                    TextSegment textSegment = content.textSegment();

                    if (textSegment != null) {
                        String text = textSegment.text();
                        String fileName = "Unknown Source";
                        int pageNumber = 0;
                        int index = 0;
                        String imageUrls = null;

                        // Extract metadata
                        Metadata metadata = textSegment.metadata();

                        // System.out.println("Metadata: " + metadata.toString());
                        if (metadata != null) {
                            fileName = metadata.getString("fileName");
                            if (fileName == null || fileName.isEmpty()) {
                                fileName = "Unknown Source";
                            }
                            // Score might be in metadata or not available
                            pageNumber = metadata.getInteger("pageNumber");
                            index = metadata.getInteger("index");
                            imageUrls = metadata.getString("imageUrls");
                        }

                        // Store source info
                        sourcesStorage.get().add(new SourceInfo(fileName, text, imageUrls, pageNumber, index, i + 1));
                    }
                }

                LOG.info("Retrieved " + sourcesStorage.get().size() + " sources");
            }

            return contents;
        }
    }

    // Record to hold source information
    public record SourceInfo(
            String fileName,
            String content,
            String imageUrls,
            int pageNumber,
            int index,
            int sourceNumber) {
    }
}