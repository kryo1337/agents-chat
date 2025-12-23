package com.kryo.agents.models.openai;

import java.util.Map;

public record Function(
    String name,
    String description,
    Map<String, Object> parameters) {
}
