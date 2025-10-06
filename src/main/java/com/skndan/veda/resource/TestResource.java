package com.skndan.veda.resource;

import com.skndan.veda.service.TestService;
import com.skndan.veda.entity.Paged;
import com.skndan.veda.entity.Ticket;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * REST endpoint for managing test tickets and theatres.
 * Provides operations to retrieve, create, and manage test data.
 * Note: Currently commented out path suggests this might be a test or development resource.
 */
@Path("/ticket")
@Tag(name = "Test Tickets", description = "Operations for testing ticket management")
public class TestResource {

    @Inject
    TestService testService;

    /**
     * Retrieves a specific ticket by its unique identifier.
     *
     * @param id the unique identifier of the ticket
     * @return the ticket with the specified ID
     */
    @GET
    @Path("{id}")
    @Operation(
        summary = "Retrieve a specific ticket",
        description = "Fetches a ticket by its unique identifier"
    )
    @APIResponse(
        responseCode = "200",
        description = "Ticket successfully retrieved",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Ticket.class))
    )
    public Ticket getTicket(
        @Parameter(description = "Unique identifier of the ticket", required = true)
        @PathParam("id") Long id
    ) {
        return testService.getTicket(id);
    }

    /**
     * Retrieves a paginated list of tickets, optionally filtered by theatre.
     *
     * @param pageNo the page number for pagination (default: 0)
     * @param pageSize number of items per page (default: 25)
     * @param theatreId optional theatre ID to filter tickets
     * @return a paginated list of tickets
     */
    @GET
    @Path("/filter")
    @Operation(
        summary = "Retrieve filtered tickets",
        description = "Fetches a paginated list of tickets, optionally filtered by theatre"
    )
    @APIResponse(
        responseCode = "200",
        description = "Tickets successfully retrieved",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Paged.class))
    )
    public Paged<Ticket> getTickets(
        @Parameter(description = "Page number", example = "0")
        @QueryParam("pageNo") @DefaultValue("0") int pageNo,
        @Parameter(description = "Number of items per page", example = "25")
        @QueryParam("pageSize") @DefaultValue("25") int pageSize,
        @Parameter(description = "Theatre ID for filtering", example = "0")
        @QueryParam("theatreId") @DefaultValue("0") Long theatreId
    ) {
        return testService.getTickets2(pageNo, pageSize, theatreId);
    }

    /**
     * Retrieves a paginated list of tickets with optional sorting.
     *
     * @param pageNo the page number for pagination (default: 0)
     * @param pageSize number of items per page (default: 25)
     * @param sortField field to sort by (default: "createdAt")
     * @param sortDir sort direction (default: "ASC")
     * @return a paginated list of tickets
     */
    @GET
    @Operation(
        summary = "Retrieve all tickets",
        description = "Fetches a paginated list of all tickets"
    )
    @APIResponse(
        responseCode = "200",
        description = "Tickets successfully retrieved",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Paged.class))
    )
    public Paged<Ticket> getTickets2(
        @Parameter(description = "Page number", example = "0")
        @QueryParam("pageNo") @DefaultValue("0") int pageNo,
        @Parameter(description = "Number of items per page", example = "25")
        @QueryParam("pageSize") @DefaultValue("25") int pageSize,
        @Parameter(description = "Field to sort by", example = "createdAt")
        @QueryParam("sortField") @DefaultValue("createdAt") String sortField,
        @Parameter(description = "Sort direction", example = "ASC")
        @QueryParam("sortDir") @DefaultValue("ASC") String sortDir
    ) {
        return testService.getTickets(pageNo, pageSize);
    }

    /**
     * Creates a new ticket in the system.
     *
     * @param ticket the ticket to be created
     * @return the newly created ticket
     */
    @POST
    @Operation(
        summary = "Create a new ticket",
        description = "Creates a new ticket in the system"
    )
    @APIResponse(
        responseCode = "200",
        description = "Ticket successfully created",
        content = @Content(mediaType = "application/json",
            schema = @Schema(implementation = Ticket.class))
    )
    public Ticket createTicket(
        @Parameter(description = "Ticket details")
        Ticket ticket
    ) {
        return testService.createTicket(ticket);
    } 
}
