package com.skndan.veda.resource;

import com.skndan.veda.entity.Paged;
import com.skndan.veda.entity.Workspace;
import com.skndan.veda.repo.WorkspaceRepo;
import com.skndan.veda.utils.EntityCopyUtils;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/v1/workspace")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Workspace", description = "Management of user workspaces")
public class WorkspaceResource {

  @Inject
  JsonWebToken jwt;

  @Inject
  WorkspaceRepo repo;

  @Inject
  EntityCopyUtils entityCopyUtils;

  @POST
  @Transactional
  public Response createWorkspace(@Valid Workspace workspace) {
    workspace.ownerId = UUID.fromString(jwt.getSubject());
    workspace.persist();
    return Response.status(Response.Status.CREATED).entity(workspace).build();
  }

  @GET
  @Path("/{id}")
  public Response getWorkspace(@PathParam("id") long id) {
    Workspace workspace = repo.findById(id);
    if (workspace == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    // Ensure the user can only access their own workspaces
    UUID ownerId = UUID.fromString(jwt.getSubject());
    if (!workspace.ownerId.equals(ownerId)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    return Response.ok(workspace).build();
  }

  @GET
  public Response listWorkspaces(
      @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("10") int size) {
    UUID ownerId = UUID.fromString(jwt.getSubject());
    Paged<Workspace> workspaces = repo.findByOwnerId(ownerId, page, size);
    return Response.ok(workspaces).build();
  }

  @PUT
  @Path("/{id}")
  @Transactional
  public Response updateWorkspace(
      @PathParam("id") long id,
      @Valid Workspace workspace) {
    Workspace existingWorkspace = repo.findById(id);

    if (existingWorkspace == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    // Ensure the user can only update their own workspaces
    UUID ownerId = UUID.fromString(jwt.getSubject());
    if (!existingWorkspace.ownerId.equals(ownerId)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    entityCopyUtils.copyProperties(existingWorkspace, workspace);
    existingWorkspace.persist();

    return Response.ok(existingWorkspace).build();
  }

  @DELETE
  @Path("/{id}")
  @Transactional
  public Response deleteWorkspace(@PathParam("id") long id) {
    Workspace workspace = repo.findById(id);
    if (workspace == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    // Ensure the user can only delete their own workspaces
    UUID ownerId = UUID.fromString(jwt.getSubject());
    if (!workspace.ownerId.equals(ownerId)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    repo.delete(workspace);
    return Response.noContent().build();
  }
}
