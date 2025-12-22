package com.kryo.agents.controllers;

import com.kryo.agents.agents.Agent;
import com.kryo.agents.agents.AgentOrchestrator;
import com.kryo.agents.models.ChatMessage;
import com.kryo.agents.models.ChatRequest;
import com.kryo.agents.models.ChatResponse;
import com.kryo.agents.models.Role;
import com.kryo.agents.services.ConversationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

  private final AgentOrchestrator orchestrator;
  private final ConversationService conversationService;

  public ChatController(AgentOrchestrator orchestrator,
      ConversationService conversationService) {
    this.orchestrator = orchestrator;
    this.conversationService = conversationService;
  }

  @PostMapping
  public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
    String conversationId = request.conversationId();
    String userMessage = request.message();

    conversationService.addMessage(conversationId, new ChatMessage(Role.USER, userMessage));

    Agent agent = orchestrator.route(userMessage);
    String reply = agent.respond(userMessage);

    conversationService.addMessage(conversationId, new ChatMessage(Role.ASSISTANT, reply));

    ChatResponse response = new ChatResponse(conversationId, agent.getName(), reply);
    return ResponseEntity.ok(response);
  }
}
