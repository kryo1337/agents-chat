package com.kryo.agents.models.openai;

import com.kryo.agents.models.Role;

import java.util.List;

public record OpenAIRequest(
    List<Message> messages,
    double temperature,
    int max_tokens
// TODO: add tools/functions
) {
  public OpenAIRequest(List<Message> messages) {
    this(messages, 0.7, 800);
  }
}
