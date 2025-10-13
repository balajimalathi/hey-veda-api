package com.skndan.veda.resource;

import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.skndan.veda.entity.Users;
import com.skndan.veda.entity.Paged;
import com.skndan.veda.repo.UserRepo;
import com.skndan.veda.utils.EntityCopyUtils;

@Path("/v1/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "Operations on Users resource.")
public class UserResource {

  @Inject
  JsonWebToken jwt;

  @Inject
  UserRepo repo;

  @Inject
  EntityCopyUtils entityCopyUtils;

  @POST
  @Transactional
  public Response create(@Valid Users group) {
    System.out.println("JWT Subject: " + jwt.getSubject());
    group.ownerId = UUID.fromString(jwt.getSubject());
    group.persist();
    return Response.status(Response.Status.CREATED).entity(group).build();
  }

  @GET
  @Path("/{id}")
  public Response get(@PathParam("id") long id) {
    Users group = repo.findById(id);
    if (group == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    // Ensure the user can only access their own workspaces
    UUID ownerId = UUID.fromString(jwt.getSubject());
    if (!group.ownerId.equals(ownerId)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    return Response.ok(group).build();
  }

  @GET
  public Response list(
      @QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("10") int size) {
    UUID ownerId = UUID.fromString(jwt.getSubject());
    Paged<Users> workspaces = repo.findByOwnerId(ownerId, page, size);
    return Response.ok(workspaces).build();
  }

  @PUT
  @Path("/{id}")
  @Transactional
  public Response update(
      @PathParam("id") long id,
      @Valid Users group) {
    Users existingUser = repo.findById(id);

    if (existingUser == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    // Ensure the user can only update their own workspaces
    UUID ownerId = UUID.fromString(jwt.getSubject());
    if (!existingUser.ownerId.equals(ownerId)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    entityCopyUtils.copyProperties(existingUser, group);
    existingUser.persist();

    return Response.ok(existingUser).build();
  }

  @DELETE
  @Path("/{id}")
  @Transactional
  public Response delete(@PathParam("id") long id) {
    Users group = repo.findById(id);
    if (group == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    // Ensure the user can only delete their own workspaces
    UUID ownerId = UUID.fromString(jwt.getSubject());
    if (!group.ownerId.equals(ownerId)) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    repo.delete(group);
    return Response.noContent().build();
  }
}
