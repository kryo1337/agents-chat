package com.kryo.agents.agents;

import com.kryo.agents.models.ChatMessage;
import java.util.List;

public interface Agent {
  String getName();

  String respond(String message, List<ChatMessage> history);
}
