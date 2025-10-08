package com.skndan.veda.service;

import org.jboss.logging.Logger;

import com.skndan.veda.config.RequestLoggingFilter;
import com.skndan.veda.config.TenantContext;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.AugmentationRequest;
import dev.langchain4j.rag.AugmentationResult;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

/**
 * Tenant-aware content retriever that queries Qdrant with tenant filtering.
 */
@ApplicationScoped
public class DocumentRetriever implements RetrievalAugmentor {

    private static final Logger LOG = Logger.getLogger(RequestLoggingFilter.class.getName());

    @Inject
    Provider<TenantContext> tenantContextProvider;

    private final QdrantEmbeddingStore store;
    private final EmbeddingModel model;

    @Inject
    public DocumentRetriever(QdrantEmbeddingStore store, EmbeddingModel model) {
        this.store = store;
        this.model = model;
    }

    @Override
    public AugmentationResult augment(AugmentationRequest augmentationRequest) {

        String tenantId = tenantContextProvider.get().getTenantId();

        LOG.info(tenantId + " - Augmenting with tenant-aware retriever");

        EmbeddingStoreContentRetriever contentRetriever = EmbeddingStoreContentRetriever.builder()
                .embeddingModel(model)
                .embeddingStore(store)
                .maxResults(3)
                .filter(MetadataFilterBuilder.metadataKey("tenantId").isEqualTo(tenantId))
                .build();

        RetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();

        return augmentor.augment(augmentationRequest);
    }

}