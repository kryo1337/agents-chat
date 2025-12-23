package com.kryo.agents.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kryo.agents.models.openai.Message;
import com.kryo.agents.models.openai.ToolCall;
import com.kryo.agents.services.AzureOpenAIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class BillingAgent implements Agent {

  private static final Logger logger = LoggerFactory.getLogger(BillingAgent.class);
  private static final Set<String> VALID_PLANS = Set.of("Basic", "Pro", "Enterprise");
  private static final int MAX_CUSTOMER_ID_LENGTH = 100;
  private static final int MAX_REASON_LENGTH = 500;

  private final AzureOpenAIService openAIService;
  private final ObjectMapper objectMapper;

  public BillingAgent(AzureOpenAIService openAIService, ObjectMapper objectMapper) {
    this.openAIService = openAIService;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getName() {
    return "billing";
  }

  @Override
  public String respond(String message) {
    // TODO: Implement tool definition and initial LLM call
    return "BILLING AGENT: ...";
  }

  private void validateBillingArguments(String functionName, JsonNode args) {
    if (args.has("customerId")) {
      String customerId = args.get("customerId").asText();
      if (customerId == null || customerId.isBlank() || customerId.length() >= MAX_CUSTOMER_ID_LENGTH) {
        logger.warn("Security validation failed: Invalid customerId '{}' for function '{}'", customerId, functionName);
        throw new IllegalArgumentException(
            "Invalid customerId: must be non-empty and under " + MAX_CUSTOMER_ID_LENGTH + " characters");
      }
    }

    if (args.has("newPlan")) {
      String newPlan = args.get("newPlan").asText();
      if (!VALID_PLANS.contains(newPlan)) {
        logger.warn("Security validation failed: Invalid plan '{}' for function '{}'", newPlan, functionName);
        throw new IllegalArgumentException("Invalid plan: must be one of " + VALID_PLANS);
      }
    }

    if (args.has("reason")) {
      String reason = args.get("reason").asText();
      if (reason == null || reason.isBlank() || reason.length() >= MAX_REASON_LENGTH) {
        logger.warn("Security validation failed: Invalid reason length for function '{}'", functionName);
        throw new IllegalArgumentException(
            "Invalid reason: must be non-empty and under " + MAX_REASON_LENGTH + " characters");
      }
    }
  }
}
