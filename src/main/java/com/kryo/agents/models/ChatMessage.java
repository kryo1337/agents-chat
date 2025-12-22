package com.kryo.agents.models;

public record ChatMessage(
    Role role,
    String content) {
}
