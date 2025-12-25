package com.kryo.agents.agents;

import com.kryo.agents.models.ChatMessage;
import com.kryo.agents.services.AzureOpenAIService;
import com.kryo.agents.services.ConversationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentOrchestrator {

  private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);
  private static final int MAX_TRACKED_CONVERSATIONS = 100;

  private final Map<String, Agent> agentMap;
  private final Map<String, String> conversationAgentMap;
  private final AzureOpenAIService openAIService;
  private final ConversationService conversationService;

  public AgentOrchestrator(List<Agent> agents, AzureOpenAIService openAIService,
      ConversationService conversationService) {
    this.agentMap = agents.stream()
        .collect(Collectors.toMap(Agent::getName, Function.identity()));
    this.openAIService = openAIService;
    this.conversationService = conversationService;
    this.conversationAgentMap = Collections.synchronizedMap(
        new LinkedHashMap<String, String>(MAX_TRACKED_CONVERSATIONS, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > MAX_TRACKED_CONVERSATIONS;
          }
        });
  }

  public Agent route(String userMessage) {
    return route(null, userMessage);
  }

  public Agent route(String conversationId, String userMessage) {
    if (userMessage == null || userMessage.trim().isEmpty()) {
      logger.warn("Received empty or null message, routing to RouterAgent");
      return agentMap.get("router");
    }

    String currentAgent = getCurrentAgent(conversationId);

    List<ChatMessage> history = (conversationId != null)
        ? conversationService.getHistory(conversationId)
        : Collections.singletonList(new ChatMessage(com.kryo.agents.models.Role.USER, userMessage));

    if (history.isEmpty()) {
      history = Collections.singletonList(new ChatMessage(com.kryo.agents.models.Role.USER, userMessage));
    }

    String suggestedAgent = openAIService.classifyIntent(history);

    String normalizedSuggested = normalizeAgentName(suggestedAgent);
    String finalAgent = determineFinalAgent(currentAgent, normalizedSuggested, userMessage);

    if (conversationId != null) {
      updateConversationAgent(conversationId, finalAgent);
    }

    logger.debug("Routing: conversationId={}, current={}, suggested={}, final={}",
        conversationId, currentAgent, normalizedSuggested, finalAgent);

    return agentMap.getOrDefault(finalAgent, agentMap.get("router"));
  }

  private String getCurrentAgent(String conversationId) {
    if (conversationId == null) {
      return null;
    }
    return conversationAgentMap.get(conversationId);
  }

  private void updateConversationAgent(String conversationId, String agentName) {
    conversationAgentMap.put(conversationId, agentName);
  }

  private String normalizeAgentName(String agentName) {
    if (agentName == null || agentName.isBlank()) {
      return "router";
    }
    return agentName.toLowerCase().replaceAll("[^a-z]", "");
  }

  private String determineFinalAgent(String currentAgent, String suggestedAgent, String userMessage) {
    if (currentAgent == null) {
      return suggestedAgent.isEmpty() ? "router" : suggestedAgent;
    }

    if (suggestedAgent.equals(currentAgent)) {
      return currentAgent;
    }

    if ("router".equals(suggestedAgent)) {
      return currentAgent;
    }

    boolean shouldSwitch = shouldSwitchAgent(currentAgent, suggestedAgent);
    if (shouldSwitch) {
      logger.info("Agent switch: conversation moving from {} to {}", currentAgent, suggestedAgent);
      return suggestedAgent;
    }

    return currentAgent;
  }

  private boolean shouldSwitchAgent(String currentAgent, String suggestedAgent) {
    return !suggestedAgent.isEmpty() && !suggestedAgent.equals("router");
  }

  public void clearConversationAgent(String conversationId) {
    if (conversationId != null) {
      conversationAgentMap.remove(conversationId);
      logger.debug("Cleared agent tracking for conversation: {}", conversationId);
    }
  }
}
