package com.kryo.agents.agents;

import com.kryo.agents.services.AzureOpenAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentOrchestrator {

  private static final Logger logger = LoggerFactory.getLogger(AgentOrchestrator.class);
  private final Map<String, Agent> agentMap;
  private final AzureOpenAIService openAIService;

  public AgentOrchestrator(List<Agent> agents, AzureOpenAIService openAIService) {
    this.agentMap = agents.stream()
        .collect(Collectors.toMap(Agent::getName, Function.identity()));
    this.openAIService = openAIService;
  }

  public Agent route(String userMessage) {
    if (userMessage == null || userMessage.trim().isEmpty()) {
      logger.warn("Received empty or null message, routing to RouterAgent");
      return agentMap.get("router");
    }

    String agentName = openAIService.classifyIntent(userMessage);
    String normalizedName = agentName == null ? "router" : agentName.toLowerCase().replaceAll("[^a-z]", "");

    logger.debug("Routing decision: input='{}', classified='{}', normalized='{}'",
        userMessage.length() > 50 ? userMessage.substring(0, 50) + "..." : userMessage,
        agentName,
        normalizedName);

    return agentMap.getOrDefault(normalizedName, agentMap.get("router"));
  }
}
