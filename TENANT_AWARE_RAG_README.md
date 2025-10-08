# Tenant-Aware RAG System for ExcelBot

This document explains the tenant-aware Retrieval-Augmented Generation (RAG) system implemented for the ExcelBot service.

## Overview

The system ensures that when users query the ExcelBot, only documents belonging to their tenant (and optionally workspace) are retrieved from Qdrant and used to generate responses. This provides proper data isolation in a multi-tenant environment.

## Architecture Components

### 1. TenantAwareRetriever
**File:** `src/main/java/com/skndan/veda/service/TenantAwareRetriever.java`

A custom `ContentRetriever` implementation that:
- Maintains tenant context using `ThreadLocal` to ensure thread-safe tenant isolation
- Queries Qdrant with tenant-based filtering before retrieving chunks
- Supports both tenant-only and tenant+workspace filtering
- Automatically cleans up context after request completion

**Key Methods:**
```java
// Set tenant context before querying
TenantAwareRetriever.setTenantContext(String tenantId)

// Optionally set workspace context for granular filtering
TenantAwareRetriever.setWorkspaceContext(String workspaceId)

// Clear context (done automatically by ExcelBotService)
TenantAwareRetriever.clearAllContexts()
```

### 2. ExcelBotService
**File:** `src/main/java/com/skndan/veda/service/ExcelBotService.java`

A service wrapper that manages tenant context lifecycle:
- Sets tenant context before calling the ExcelBot
- Ensures context is cleared after the request (even if errors occur)
- Provides three query modes:
  - `answerWithTenant()` - Filter by tenant only
  - `answerWithTenantAndWorkspace()` - Filter by both tenant and workspace
  - `answerWithoutContext()` - No RAG, uses LLM knowledge only

### 3. ExcelBot
**File:** `src/main/java/com/skndan/veda/service/ExcelBot.java`

The AI service interface that:
- Is registered as a Quarkus LangChain4j service
- Automatically uses the registered `ContentRetriever` (TenantAwareRetriever)
- Generates answers based on retrieved context

### 4. LangChain4jConfig
**File:** `src/main/java/com/skndan/veda/config/LangChain4jConfig.java`

Configuration class that:
- Registers the `TenantAwareRetriever` as the default `ContentRetriever` bean
- Ensures the retriever is automatically injected into AI services

### 5. ExcelBotResource
**File:** `src/main/java/com/skndan/veda/resource/ExcelBotResource.java`

REST API endpoints demonstrating usage:
- `POST /api/excel-bot/query` - Query with tenant filtering
- `POST /api/excel-bot/query/workspace` - Query with tenant and workspace filtering
- `POST /api/excel-bot/query/no-context` - Query without RAG

## How It Works

### Request Flow

```
1. Client sends question with tenantId
   ↓
2. ExcelBotResource receives request
   ↓
3. ExcelBotService.answerWithTenant() is called
   ↓
4. TenantAwareRetriever.setTenantContext(tenantId) sets context
   ↓
5. ExcelBot.answer(question) is invoked
   ↓
6. LangChain4j framework calls TenantAwareRetriever.retrieve()
   ↓
7. TenantAwareRetriever queries QdrantService.searchByTenant()
   ↓
8. Qdrant returns chunks filtered by tenantId
   ↓
9. Retrieved chunks are passed to LLM as context
   ↓
10. LLM generates answer based on tenant-specific context
   ↓
11. ExcelBotService clears tenant context (in finally block)
   ↓
12. Response returned to client
```

### Multi-Tenancy Filtering

The system leverages metadata filtering in Qdrant:

```java
// When ingesting (QdrantService)
Map<String, String> metadata = {
    "tenantId": "tenant-123",
    "workspaceId": "workspace-456",
    "userId": "user-789",
    "documentType": "report",
    "fileName": "Q4-report.xlsx"
}

// When retrieving (TenantAwareRetriever)
EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
    .queryEmbedding(queryEmbedding)
    .maxResults(5)
    .filter(MetadataFilterBuilder.metadataKey("tenantId").isEqualTo(tenantId))
    .build();
```

## Usage Examples

### Example 1: Query with Tenant Context

**Request:**
```bash
curl -X POST http://localhost:8080/api/excel-bot/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "How do I use VLOOKUP?",
    "tenantId": "tenant-123"
  }'
```

**Response:**
```json
{
  "answer": "Based on your uploaded documents, VLOOKUP...",
  "tenantId": "tenant-123",
  "workspaceId": null
}
```

### Example 2: Query with Tenant and Workspace Context

**Request:**
```bash
curl -X POST http://localhost:8080/api/excel-bot/query/workspace \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Show me the sales formula from our Q4 report",
    "tenantId": "tenant-123",
    "workspaceId": "workspace-456"
  }'
```

**Response:**
```json
{
  "answer": "In your Q4 report, the sales formula is...",
  "tenantId": "tenant-123",
  "workspaceId": "workspace-456"
}
```

### Example 3: Programmatic Usage in Java

```java
@Inject
ExcelBotService excelBotService;

public void processUserQuery(String question, String tenantId) {
    // Query with tenant filtering
    String answer = excelBotService.answerWithTenant(question, tenantId);
    
    // Context is automatically managed and cleaned up
    // No need to manually clear ThreadLocal
}

public void processWorkspaceQuery(String question, String tenantId, String workspaceId) {
    // Query with tenant and workspace filtering
    String answer = excelBotService.answerWithTenantAndWorkspace(
        question, 
        tenantId, 
        workspaceId
    );
}
```

## Thread Safety

The system uses `ThreadLocal` to maintain tenant context, ensuring:
- Each request thread has its own isolated tenant context
- No cross-contamination between concurrent requests
- Automatic cleanup in `finally` blocks prevents memory leaks

## Security Considerations

1. **Tenant Isolation:** Documents from different tenants are strictly separated
2. **Metadata Filtering:** Qdrant enforces filtering at the database level
3. **Context Cleanup:** ThreadLocal is always cleared to prevent context leakage
4. **Validation:** Tenant ID is required and validated before queries

## Configuration

The system automatically integrates with Quarkus LangChain4j. No additional configuration is needed beyond:

1. Qdrant connection configuration (in `application.properties`)
2. LLM configuration (in `application.properties`)
3. Embedding model configuration (in `application.properties`)

## Extending the System

### Adding Custom Filters

To add additional filtering (e.g., by document type):

```java
// In QdrantService, already implemented:
public EmbeddingSearchResult<TextSegment> searchByTenantAndDocumentType(
    String queryText, 
    String tenantId,
    String documentType,
    int maxResults
)

// You can call this from TenantAwareRetriever by adding a new ThreadLocal
// for documentType and modifying the retrieve() method
```

### Adjusting Maximum Results

Change the `MAX_RESULTS` constant in `TenantAwareRetriever`:

```java
private static final int MAX_RESULTS = 10; // Default is 5
```

## Testing

### Unit Test Example

```java
@QuarkusTest
public class TenantAwareRetrieverTest {
    
    @Inject
    ExcelBotService excelBotService;
    
    @Inject
    QdrantService qdrantService;
    
    @Test
    public void testTenantIsolation() {
        // Ingest document for tenant A
        qdrantService.ingest("Data for tenant A", "tenant-a", ...);
        
        // Ingest document for tenant B
        qdrantService.ingest("Data for tenant B", "tenant-b", ...);
        
        // Query as tenant A - should only see tenant A data
        String answerA = excelBotService.answerWithTenant(
            "What data do I have?", 
            "tenant-a"
        );
        
        // Verify tenant A answer doesn't contain tenant B data
        assertThat(answerA).contains("tenant A").doesNotContain("tenant B");
    }
}
```

## Troubleshooting

### Issue: "Tenant ID must be set before retrieving"

**Cause:** TenantAwareRetriever was called without setting tenant context

**Solution:** Always use `ExcelBotService` methods which handle context automatically:
```java
// ❌ Don't call ExcelBot directly
excelBot.answer(question);

// ✅ Use ExcelBotService instead
excelBotService.answerWithTenant(question, tenantId);
```

### Issue: Retrieving wrong tenant's data

**Cause:** ThreadLocal context not properly cleared or reused

**Solution:** The system handles this automatically, but if you're extending it:
```java
try {
    TenantAwareRetriever.setTenantContext(tenantId);
    // Your code here
} finally {
    TenantAwareRetriever.clearAllContexts(); // Always in finally block
}
```

## Performance Considerations

1. **Caching:** Consider implementing caching for frequently asked questions per tenant
2. **Index Optimization:** Ensure Qdrant has proper indexes on `tenantId` metadata field
3. **Batch Queries:** For multiple questions, consider batching to reduce overhead
4. **Connection Pooling:** Qdrant client should use connection pooling (configured in Quarkus)

## Summary

This tenant-aware RAG system provides:
- ✅ Complete tenant isolation
- ✅ Thread-safe context management
- ✅ Automatic cleanup and error handling
- ✅ Flexible filtering (tenant-only or tenant+workspace)
- ✅ Easy-to-use REST API
- ✅ Integration with existing QdrantService multi-tenancy metadata

The system ensures that your ExcelBot only uses context from documents belonging to the requesting tenant, maintaining strict data isolation in your multi-tenant environment.