package com.kryo.agents.models;

public record ChatResponse(
        String conversationId,
        String agent,
        String reply
) {}
