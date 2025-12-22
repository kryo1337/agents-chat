package com.kryo.agents.agents;

import com.kryo.agents.services.AzureOpenAIService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentOrchestrator {

  private final Map<String, Agent> agentMap;
  private final AzureOpenAIService openAIService;

  public AgentOrchestrator(List<Agent> agents, AzureOpenAIService openAIService) {
    this.agentMap = agents.stream()
        .collect(Collectors.toMap(Agent::getName, Function.identity()));
    this.openAIService = openAIService;
  }

  public Agent route(String userMessage) {
    String agentName = openAIService.classifyIntent(userMessage);

    String normalizedName = agentName.toLowerCase().replaceAll("[^a-z]", "");

    return agentMap.getOrDefault(normalizedName, agentMap.get("router"));
  }
}
