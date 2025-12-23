package com.kryo.agents.models.openai;

public record Tool(
    String type,
    Function function) {
  public Tool(Function function) {
    this("function", function);
  }
}
