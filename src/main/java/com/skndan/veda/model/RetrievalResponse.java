package com.skndan.veda.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public record RetrievalResponse(String answer) {
  @JsonCreator
  public RetrievalResponse {
  }
}