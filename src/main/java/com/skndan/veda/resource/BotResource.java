package com.skndan.veda.resource;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.jboss.logging.Logger;

import com.skndan.veda.model.RetrievalResponse;
import com.skndan.veda.service.Bot;
import com.skndan.veda.service.QdrantRetriever;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/bot")
public class BotResource {

  public record RetrievalRequest(String question) {
  }

  private static final Logger LOG = Logger.getLogger(BotResource.class.getName());

  @Inject
  Bot bot;

  @POST
  @Path("/ask")
  @Produces(MediaType.APPLICATION_JSON)
  public Response askQuestion(@RequestBody RetrievalRequest request) {
    try {
      // Get answer from bot
      String rawResponse = bot.answer(request.question);

      // Retrieve sources from thread-local storage
      List<QdrantRetriever.SourceInfo> allSources = QdrantRetriever.getLastRetrievedSources();

      // Parse response and determine validity
      ParsedResponse parsed = parseResponse(rawResponse);

      List<RetrievalResponse.SourceReference> sourceRefs;

      if (parsed.isValid && !parsed.citedSourceNumbers.isEmpty()) {
        // Return only cited sources
        sourceRefs = allSources.stream()
            .filter(s -> parsed.citedSourceNumbers.contains(s.sourceNumber()))
            .map(s -> new RetrievalResponse.SourceReference(
                s.fileName(),
                s.content(),
                s.score(),
                s.sourceNumber()))
            .collect(Collectors.toList());
      } else {
        // Return empty sources for invalid responses
        sourceRefs = List.of();
      }

      LOG.info("Query: " + request.question + " | Valid: " + parsed.isValid +
          " | Sources: " + sourceRefs.size());

      return Response.ok(new RetrievalResponse(parsed.answer, sourceRefs)).build();

    } finally {
      // Clean up thread-local storage
      QdrantRetriever.clearSources();
    }
  }

  private ParsedResponse parseResponse(String rawResponse) {
    boolean isValid = rawResponse.contains("<<VALID>>");
    String answer = "";
    Set<Integer> citedSourceNumbers = new HashSet<>();

    // Extract answer text (between markers and <<SOURCES>>)
    Pattern answerPattern = Pattern.compile("<<(?:VALID|INVALID)>>\\s*([\\s\\S]*?)<<SOURCES>>",
        Pattern.CASE_INSENSITIVE);
    Matcher answerMatcher = answerPattern.matcher(rawResponse);
    if (answerMatcher.find()) {
      answer = answerMatcher.group(1).trim();
    } else {
      // Fallback: use entire response if markers not found
      answer = rawResponse.replaceAll("<<.*?>>", "").trim();
    }

    // Extract source numbers
    Pattern sourcesPattern = Pattern.compile("<<SOURCES>>\\s*([\\s\\S]*)", Pattern.CASE_INSENSITIVE);
    Matcher sourcesMatcher = sourcesPattern.matcher(rawResponse);
    if (sourcesMatcher.find()) {
      String sourcesText = sourcesMatcher.group(1).trim();

      // Check for NONE
      if (!sourcesText.equalsIgnoreCase("NONE") && !sourcesText.isEmpty()) {
        // Parse comma-separated numbers or individual numbers
        Pattern numberPattern = Pattern.compile("\\d+");
        Matcher numberMatcher = numberPattern.matcher(sourcesText);
        while (numberMatcher.find()) {
          citedSourceNumbers.add(Integer.parseInt(numberMatcher.group()));
        }
      }
    }

    return new ParsedResponse(isValid, answer, citedSourceNumbers);
  }

  private record ParsedResponse(boolean isValid, String answer, Set<Integer> citedSourceNumbers) {
  }

  public record ChatRequest(String question) {
  }

  @POST
  @Path("/questionnaire")
  @Produces(MediaType.APPLICATION_JSON)
  public RetrievalResponse createQuestionnaire(@RequestBody RetrievalRequest request) {
    try {
      // Get answer from bot
      String response = bot.answer(request.question);

      // Retrieve sources from thread-local storage
      List<QdrantRetriever.SourceInfo> sources = QdrantRetriever.getLastRetrievedSources();

      // Convert to SourceReference objects
      List<RetrievalResponse.SourceReference> sourceRefs = sources.stream()
          .map(s -> new RetrievalResponse.SourceReference(
              s.fileName(),
              s.content(),
              s.score(),
              s.sourceNumber()))
          .collect(Collectors.toList());

      // Return response with sources
      return new RetrievalResponse(response, sourceRefs);
    } finally {
      // Clean up thread-local storage
      QdrantRetriever.clearSources();
    }
  }

}
