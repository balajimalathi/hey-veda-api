package com.skndan.veda.resource;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.skndan.veda.entity.Profile;
import com.skndan.veda.repo.ProfileRepo;

import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/me")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {

  @Inject
  JsonWebToken jwt;

  @Inject
  ProfileRepo repo;

  @GET
  @Transactional
  public Response getOrCreateProfile() {
    String uid = jwt.getSubject();
    String email = jwt.getClaim("email");
    String name = jwt.getClaim("name");

    Profile profile = repo.findByUid(uid);
    if (profile == null) {
      profile = new Profile();
      profile.email = email;
      profile.name = name;
      profile.id = uid;
      profile.persist();
    }

    return Response.ok(profile).build();
  }
}