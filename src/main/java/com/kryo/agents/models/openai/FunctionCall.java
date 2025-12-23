package com.kryo.agents.models.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FunctionCall(
    String name,
    String arguments) {
}
