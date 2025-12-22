package com.kryo.agents.models;

public record ChatRequest(
    String conversationId,
    String message,
    String customerId) {
}
