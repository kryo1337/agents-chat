package com.kryo.agents.models.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OpenAIRequest(
    List<Message> messages,
    double temperature,
    int max_tokens,
    List<Tool> tools,
    Object tool_choice) {
  public OpenAIRequest(List<Message> messages) {
    this(messages, 0.7, 800, null, null);
  }

  public OpenAIRequest(List<Message> messages, List<Tool> tools) {
    this(messages, 0.7, 800, tools, "auto");
  }
}
