package com.kryo.agents.models.openai;

import com.kryo.agents.models.Role;

public record Message(
    String role,
    String content) {
  public static Message user(String content) {
    return new Message(Role.USER.name().toLowerCase(), content);
  }

  public static Message system(String content) {
    return new Message(Role.SYSTEM.name().toLowerCase(), content);
  }

  public static Message assistant(String content) {
    return new Message(Role.ASSISTANT.name().toLowerCase(), content);
  }
}
