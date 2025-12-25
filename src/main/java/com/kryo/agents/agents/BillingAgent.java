package com.kryo.agents.agents;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kryo.agents.models.ChatMessage;
import com.kryo.agents.models.openai.Message;
import com.kryo.agents.models.openai.Tool;
import com.kryo.agents.services.AzureOpenAIService;
import com.kryo.agents.services.BillingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BillingAgent implements Agent {

  private static final Logger logger = LoggerFactory.getLogger(BillingAgent.class);
  private static final Set<String> VALID_PLANS = Set.of("Starter", "Pro", "Enterprise");
  private static final int MAX_CUSTOMER_ID_LENGTH = 100;
  private static final int MAX_REASON_LENGTH = 500;

  private final AzureOpenAIService openAIService;
  private final BillingService billingService;
  private final ObjectMapper objectMapper;

  public BillingAgent(AzureOpenAIService openAIService, BillingService billingService,
      ObjectMapper objectMapper) {
    this.openAIService = openAIService;
    this.billingService = billingService;
    this.objectMapper = objectMapper;
  }

  @Override
  public String getName() {
    return "billing";
  }

  @Override
  public String respond(String message, List<ChatMessage> history) {
    String systemPrompt = """
        You are a billing support specialist.

        Your capabilities:
        - Check subscription details (plan, pricing, billing cycle)
        - Initiate refund requests and generate support tickets
        - Explain refund policy and timelines
        - Change subscription plans

        Guidelines:
        - ALWAYS ask for customerId if needed before calling tools
        - Be helpful but follow refund policy strictly
        - Provide clear, accurate information from tool results
        - If a customer is not eligible for a refund, explain the policy gently

        Available tools: checkSubscription, initiateRefund, explainRefundPolicy, changePlan
        """;

    List<Tool> tools = List.of(
        buildCheckSubscriptionTool(),
        buildInitiateRefundTool(),
        buildExplainRefundPolicyTool(),
        buildChangePlanTool());

    List<Message> conversation = new ArrayList<>();
    conversation.add(Message.system(systemPrompt));

    for (ChatMessage msg : history) {
      conversation.add(new Message(msg.role().name().toLowerCase(), msg.content()));
    }

    if (history.isEmpty() || !history.get(history.size() - 1).content().equals(message)) {
      conversation.add(Message.user(message));
    }

    try {
      Message response = openAIService.executeToolCallLoop(conversation, tools,
          this::executeToolCall);
      return response != null && response.content() != null ? response.content()
          : "I apologize, I could not generate a response.";
    } catch (Exception e) {
      logger.error("Billing agent response failed", e);
      return "I apologize, but I am encountering an error while processing your request. Please try again.";
    }
  }

  public String executeToolCall(String toolName, String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      validateBillingArguments(toolName, args);

      return switch (toolName) {
        case "checkSubscription" -> executeCheckSubscription(args);
        case "initiateRefund" -> executeInitiateRefund(args);
        case "explainRefundPolicy" -> executeExplainRefundPolicy();
        case "changePlan" -> executeChangePlan(args);
        default -> throw new IllegalArgumentException("Unknown tool: " + toolName);
      };
    } catch (Exception e) {
      logger.error("Tool execution failed: tool={}, error={}", toolName, e.getMessage());
      throw new RuntimeException("Tool execution failed: " + e.getMessage(), e);
    }
  }

  private void validateBillingArguments(String functionName, JsonNode args) {
    if (args.has("customerId")) {
      String customerId = args.get("customerId").asText();
      if (customerId == null || customerId.isBlank() || customerId.length() >= MAX_CUSTOMER_ID_LENGTH) {
        logger.warn("Security validation failed: Invalid customerId '{}' for function '{}'", customerId,
            functionName);
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

  private String executeCheckSubscription(JsonNode args) {
    String customerId = args.get("customerId").asText();
    BillingService.SubscriptionDetails details = billingService.checkSubscription(customerId);

    Map<String, Object> result = new HashMap<>();
    result.put("customerId", details.customerId());
    result.put("plan", details.plan());
    result.put("price", details.price());
    result.put("billingCycle", details.billingCycle());
    result.put("startDate", details.startDate());
    result.put("renewalDate", details.renewalDate());

    return toJson(result);
  }

  private String executeInitiateRefund(JsonNode args) {
    String customerId = args.get("customerId").asText();
    String reason = args.get("reason").asText();
    BillingService.RefundResult result = billingService.initiateRefund(customerId, reason);

    Map<String, Object> refundInfo = new HashMap<>();
    refundInfo.put("ticketId", result.ticketId());
    refundInfo.put("formUrl", result.formUrl());
    refundInfo.put("refundAmount", result.refundAmount());
    refundInfo.put("policyExplanation", result.policyExplanation());

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("refundInfo", refundInfo);

    return toJson(response);
  }

  private String executeExplainRefundPolicy() {
    BillingService.RefundPolicy policy = billingService.explainRefundPolicy();

    Map<String, Object> result = new HashMap<>();
    result.put("fullRefundWindow", policy.fullRefundWindow());
    result.put("partialRefundWindow", policy.partialRefundWindow());
    result.put("partialRefundPercentage", policy.partialRefundPercentage());
    result.put("noRefundAfter", policy.noRefundAfter());

    return toJson(result);
  }

  private String executeChangePlan(JsonNode args) {
    String customerId = args.get("customerId").asText();
    String newPlan = args.get("newPlan").asText();
    BillingService.PlanChangeResult result = billingService.changePlan(customerId, newPlan);

    Map<String, Object> response = new HashMap<>();
    response.put("success", true);
    response.put("customerId", result.customerId());
    response.put("previousPlan", result.previousPlan());
    response.put("newPlan", result.newPlan());
    response.put("message", result.message());
    response.put("effectiveDate", result.effectiveDate());

    return toJson(response);
  }

  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception e) {
      logger.error("Failed to serialize response to JSON", e);
      throw new RuntimeException("Failed to serialize response", e);
    }
  }

  private Tool buildCheckSubscriptionTool() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("type", "object");

    Map<String, Object> properties = new HashMap<>();
    properties.put("customerId", Map.of(
        "type", "string",
        "description", "Customer unique identifier (e.g., customer-001, customer-002)"));
    parameters.put("properties", properties);
    parameters.put("required", List.of("customerId"));

    return new Tool(new com.kryo.agents.models.openai.Function(
        "checkSubscription",
        "Get customer's subscription plan, pricing, and billing cycle information",
        parameters));
  }

  private Tool buildInitiateRefundTool() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("type", "object");

    Map<String, Object> properties = new HashMap<>();
    properties.put("customerId", Map.of(
        "type", "string",
        "description", "Customer unique identifier"));
    properties.put("reason", Map.of(
        "type", "string",
        "description", "Reason for refund request"));
    parameters.put("properties", properties);
    parameters.put("required", List.of("customerId", "reason"));

    return new Tool(new com.kryo.agents.models.openai.Function(
        "initiateRefund",
        "Initiate a refund request and generate a support ticket with form URL",
        parameters));
  }

  private Tool buildExplainRefundPolicyTool() {
    return new Tool(new com.kryo.agents.models.openai.Function(
        "explainRefundPolicy",
        "Get the refund policy details including timeframes and percentages",
        Map.of(
            "type", "object",
            "properties", Map.of()
        )));
  }

  private Tool buildChangePlanTool() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("type", "object");

    Map<String, Object> properties = new HashMap<>();
    properties.put("customerId", Map.of(
        "type", "string",
        "description", "Customer unique identifier"));
    properties.put("newPlan", Map.of(
        "type", "string",
        "description", "New plan name: Starter, Pro, or Enterprise",
        "enum", List.of("Starter", "Pro", "Enterprise")));
    parameters.put("properties", properties);
    parameters.put("required", List.of("customerId", "newPlan"));

    return new Tool(new com.kryo.agents.models.openai.Function(
        "changePlan",
        "Change customer's subscription plan and provide proration details",
        parameters));
  }
}
