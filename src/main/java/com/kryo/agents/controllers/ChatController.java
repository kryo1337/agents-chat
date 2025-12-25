package com.kryo.agents.controllers;

import com.kryo.agents.agents.Agent;
import com.kryo.agents.agents.AgentOrchestrator;
import com.kryo.agents.models.ChatMessage;
import com.kryo.agents.models.ChatRequest;
import com.kryo.agents.models.ChatResponse;
import com.kryo.agents.models.Role;
import com.kryo.agents.services.ConversationService;
import com.kryo.agents.exceptions.AiCallException;
import org.springframework.http.HttpStatus;
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
  public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
    if (request.conversationId() == null || request.conversationId().isBlank()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "conversationId cannot be null or blank"));
    }
    if (request.message() == null || request.message().isBlank()) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "message cannot be null or blank"));
    }
    if (request.message().length() > 8000) {
      return ResponseEntity.badRequest().body(java.util.Map.of("error", "message cannot exceed 8000 characters"));
    }

    String conversationId = request.conversationId();
    String userMessage = request.message();

    conversationService.addMessage(conversationId, new ChatMessage(Role.USER, userMessage));

    Agent agent = orchestrator.route(conversationId, userMessage);

    java.util.List<ChatMessage> history = conversationService.getHistory(conversationId);
    String reply = agent.respond(userMessage, history);

    conversationService.addMessage(conversationId, new ChatMessage(Role.ASSISTANT, reply));

    ChatResponse response = new ChatResponse(conversationId, agent.getName(), reply);
    return ResponseEntity.ok(response);
  }

  @ExceptionHandler(AiCallException.class)
  public ResponseEntity<String> handleAiCallException(AiCallException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("AI Service Error: " + e.getMessage());
  }
}
