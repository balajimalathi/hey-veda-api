package com.skndan.veda.resource;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

import com.skndan.veda.model.RetrievalResponse;
import com.skndan.veda.service.Bot;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/v1/bot")
public class BotResource {

  public record RetrievalRequest(String question) {
  }

  @Inject
  Bot bot;

  @POST
  @Path("/ask")
  @Produces(MediaType.APPLICATION_JSON)
  public RetrievalResponse askQuestion(@RequestBody RetrievalRequest request) {
    return bot.answer(request.question);
  }

  @POST
  @Path("/questionnaire")
  @Produces(MediaType.APPLICATION_JSON)
  public RetrievalResponse createQuestionnaire(@RequestBody RetrievalRequest request) {
    return bot.answer(request.question);
  }

}
