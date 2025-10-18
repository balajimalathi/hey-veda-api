package com.skndan.veda.resource;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import com.skndan.veda.config.TenantContext;
import com.skndan.veda.service.Bot;
import com.skndan.veda.service.QdrantService;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v1/hello")
public class GreetingResource {

    /**
     * Record to represent the upload URL response
     */
    public record IngestionRequest(String content, String tenantId, String workspaceId, String fileName) {
    }

    public record RetrievalRequest(String question) {
    }

    @Inject
    Bot bot;

    @Inject
    TenantContext tenantContext;

    @Inject
    QdrantService qdrantService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public String ingestion(@RequestBody IngestionRequest request) {

        // Updated to include multi-tenancy metadata
        // qdrantService.ingest(
        //         request.content,
        //         request.tenantId, // tenantId
        //         request.workspaceId, // workspaceId
        //         "f9ecebdd-03d6-4d6d-b210-4d24ee719b9d", // userId
        //         request.fileName // fileName
        // );

        return "Hello from Quarkus REST";
    }

    @GET
    @Path("/tenant")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello2() {
        String tenantId = tenantContext.getTenantId();
        System.out.println("Tenant ID in resource: " + tenantId);
        return "Hello from Quarkus REST" + tenantId;
    }

}
