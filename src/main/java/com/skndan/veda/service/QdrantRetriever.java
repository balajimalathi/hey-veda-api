package com.skndan.veda.service;

import java.util.function.Supplier;

import org.jboss.logging.Logger;

import com.skndan.veda.config.RequestLoggingFilter;
import com.skndan.veda.config.TenantContext;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
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

    @Inject
    QdrantRetriever(QdrantEmbeddingStore store, EmbeddingModel model) {
        this.store = store;
        this.model = model;
    }

    @Override
    public RetrievalAugmentor get() {
        String tenantId = tenantContextProvider.get().getTenantId();

        LOG.info(tenantId + " - Augmenting with tenant-aware retriever");

        EmbeddingStoreContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .maxResults(10)
                .minScore(0.7) // Only return results with good similarity score
                .filter(MetadataFilterBuilder.metadataKey("tenantId").isEqualTo(tenantId))
                .build();

        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(retriever)
                .build();
    }
}