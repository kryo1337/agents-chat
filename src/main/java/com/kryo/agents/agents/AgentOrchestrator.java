package com.kryo.agents.agents;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentOrchestrator {

    private final Map<String, Agent> agentMap;

    public AgentOrchestrator(List<Agent> agents) {
        this.agentMap = agents.stream()
                .collect(Collectors.toMap(Agent::getName, Function.identity()));
    }

    public Agent route(String userMessage) {
        String agentName = decideAgentName(userMessage);
        return agentMap.getOrDefault(agentName, agentMap.get("router"));
    }

    private String decideAgentName(String userMessage) {
        String lower = userMessage.toLowerCase();

        if (lower.contains("invoice") || lower.contains("billing") || lower.contains("refund") || lower.contains("payment")) {
            return "billing";
        }

        if (lower.contains("error") || lower.contains("api") || lower.contains("integration") || lower.contains("setup")) {
            return "technical";
        }

        return "router";
    }
}