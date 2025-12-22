package com.kryo.agents.models.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAIResponse(
    String id,
    List<Choice> choices,
    Usage usage) {
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Choice(
      int index,
      Message message,
      String finish_reason) {
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Usage(
      int prompt_tokens,
      int completion_tokens,
      int total_tokens) {
  }
}
