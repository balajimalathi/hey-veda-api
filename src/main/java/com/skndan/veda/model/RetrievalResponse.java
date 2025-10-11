package com.skndan.veda.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

public record RetrievalResponse(
    String answer,
    List<SourceReference> sources
) {
  @JsonCreator
  public RetrievalResponse {
  }
  
  public record SourceReference(
      String fileName,
      String excerpt,
      Double score,
      int sourceNumber
  ) {}
}
