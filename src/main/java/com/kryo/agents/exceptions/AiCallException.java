package com.kryo.agents.exceptions;

public class AiCallException extends RuntimeException {
  public AiCallException(String message) {
    super(message);
  }

  public AiCallException(String message, Throwable cause) {
    super(message, cause);
  }
}
