package com.kryo.agents.controllers;

import com.kryo.agents.agents.Agent;
import com.kryo.agents.agents.AgentOrchestrator;
import com.kryo.agents.models.ChatMessage;
import com.kryo.agents.models.ChatRequest;
import com.kryo.agents.models.ChatResponse;
import com.kryo.agents.models.ConversationSummary;
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
  public ResponseEntity<?> chat(@RequestBody ChatRequest request,
      @RequestHeader(value = "X-User-ID", required = false) String userId) {
    if (userId == null || userId.isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(java.util.Map.of("error", "Unauthorized: Missing X-User-ID header"));
    }
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

    conversationService.addMessage(conversationId, userId, new ChatMessage(Role.USER, userMessage));

    Agent agent = orchestrator.route(conversationId, userMessage);

    java.util.List<ChatMessage> history = conversationService.getRecentHistory(conversationId,
        com.kryo.agents.config.AppConstants.MAX_CONTEXT_MESSAGES);
    String reply = agent.respond(userMessage, history);

    conversationService.addMessage(conversationId, userId, new ChatMessage(Role.ASSISTANT, reply));

    ChatResponse response = new ChatResponse(conversationId, agent.getName(), reply);
    return ResponseEntity.ok(response);
  }

  @ExceptionHandler(AiCallException.class)
  public ResponseEntity<String> handleAiCallException(AiCallException e) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("AI Service Error: " + e.getMessage());
  }

  @GetMapping("/conversations")
  public ResponseEntity<?> getConversations(@RequestHeader(value = "X-User-ID", required = false) String userId) {
    if (userId == null || userId.isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized: Missing X-User-ID header");
    }
    return ResponseEntity.ok(conversationService.getSummaries(userId));
  }

  @GetMapping("/conversations/{id}")
  public ResponseEntity<java.util.List<ChatMessage>> getConversation(@PathVariable String id,
      @RequestHeader(value = "X-User-ID", required = false) String userId) {
    if (userId == null || userId.isBlank()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    return conversationService.findHistory(id, userId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
