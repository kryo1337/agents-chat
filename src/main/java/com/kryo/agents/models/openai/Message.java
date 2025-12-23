package com.kryo.agents.models.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kryo.agents.models.Role;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Message(
    String role,
    String content,
    String tool_call_id,
    List<ToolCall> tool_calls) {

  public Message(String role, String content) {
    this(role, content, null, null);
  }

  public static Message user(String content) {
    return new Message(Role.USER.name().toLowerCase(), content);
  }

  public static Message system(String content) {
    return new Message(Role.SYSTEM.name().toLowerCase(), content);
  }

  public static Message assistant(String content) {
    return new Message(Role.ASSISTANT.name().toLowerCase(), content);
  }

  public static Message assistant(String content, List<ToolCall> toolCalls) {
    return new Message(Role.ASSISTANT.name().toLowerCase(), content, null, toolCalls);
  }

  public static Message tool(String content, String toolCallId) {
    return new Message("tool", content, toolCallId, null);
  }
}
