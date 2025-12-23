package com.kryo.agents.models.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ToolCall(
    String id,
    String type,
    FunctionCall function) {
}
