package com.kryo.agents.services;

import com.kryo.agents.exceptions.AiCallException;
import com.kryo.agents.models.ChatMessage;
import com.kryo.agents.models.openai.Message;
import com.kryo.agents.models.openai.OpenAIRequest;
import com.kryo.agents.models.openai.OpenAIResponse;
import com.kryo.agents.models.openai.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class AzureOpenAIService {

  private static final Logger logger = LoggerFactory.getLogger(AzureOpenAIService.class);
  private final RestClient restClient;

  @Value("${azure.openai.deployment-name}")
  private String deploymentName;

  @Value("${azure.openai.api-version}")
  private String apiVersion;

  public AzureOpenAIService(RestClient restClient) {
    this.restClient = restClient;
  }

  public String classifyIntent(String userMessage) {
    return classifyIntent(List.of(new ChatMessage(com.kryo.agents.models.Role.USER, userMessage)));
  }

  public String classifyIntent(List<ChatMessage> history) {
    String systemPrompt = """
        You are a helpful support routing assistant.
        Classify the conversation intent into one of these categories based on the latest user message and context:
        - "technical": for API errors, integration issues, setup, bugs, documentation, technical questions about the project (e.g., "429 error", "how to run").
        - "billing": for invoices, refunds, payments, subscription plans, upgrading/downgrading, or if the user provides a customer ID (e.g., "customer-001", "check my account").
        - "router": if it's general chitchat, greeting, or completely unrelated.

        Return ONLY the category name in lowercase (e.g., "technical", "billing", "router").
        Do NOT answer the question.
        Do NOT provide explanations.
        Output a SINGLE WORD.
        If the user mentions a customer ID or "account", it is almost always "billing".
        If the user asks about an error code or "how to", it is "technical".
        """;

    List<Message> messages = new java.util.ArrayList<>();
    messages.add(Message.system(systemPrompt));

    int start = Math.max(0, history.size() - 5);
    for (int i = start; i < history.size(); i++) {
      ChatMessage cm = history.get(i);
      messages.add(new Message(cm.role().name().toLowerCase(), cm.content()));
    }

    try {
      Message result = sendRequest(messages);
      return result != null && result.content() != null ? result.content().trim() : "router";
    } catch (AiCallException e) {
      logger.warn("Intent classification failed, defaulting to router. Error: {}", e.getMessage());
      return "router";
    }
  }

  public String chat(List<com.kryo.agents.models.ChatMessage> history) {
    List<Message> messages = history.stream()
        .map(msg -> new Message(msg.role().name().toLowerCase(), msg.content()))
        .toList();

    try {
      Message result = sendRequest(messages);
      return result != null && result.content() != null ? result.content().trim()
          : "I apologize, but I am currently unavailable.";
    } catch (AiCallException e) {
      logger.error("Chat request failed", e);
      return "I apologize, but I am encountering technical difficulties. Please try again later.";
    }
  }

  public Message sendRequest(List<Message> messages) {
    return sendRequest(messages, null);
  }

  public Message sendRequest(List<Message> messages, List<Tool> tools) {
    OpenAIRequest request = (tools == null || tools.isEmpty())
        ? new OpenAIRequest(messages)
        : new OpenAIRequest(messages, tools);

    try {
      OpenAIResponse response = restClient.post()
          .uri("/openai/deployments/{deployment}/chat/completions?api-version={version}",
              deploymentName, apiVersion)
          .body(request)
          .retrieve()
          .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, resp) -> {
            try (var body = resp.getBody()) {
              String errorBody = new String(body.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
              logger.error("OpenAI API Error: Status={}, Body={}", resp.getStatusCode(), errorBody);
            } catch (IOException e) {
              logger.error("Failed to read error response body", e);
            }
            throw new AiCallException("OpenAI API call failed with status: " + resp.getStatusCode());
          })
          .body(OpenAIResponse.class);

      return Optional.ofNullable(response)
          .filter(r -> r.choices() != null && !r.choices().isEmpty())
          .map(r -> r.choices().get(0))
          .filter(choice -> choice.message() != null)
          .map(choice -> choice.message())
          .orElseThrow(
              () -> new AiCallException("OpenAI returned an invalid response structure (missing choices or message)"));

    } catch (AiCallException e) {
      throw e;
    } catch (Exception e) {
      logger.error("Unexpected error during OpenAI API call", e);
      throw new AiCallException("Unexpected error during OpenAI interaction", e);
    }
  }

  public boolean hasToolCalls(Message message) {
    return message.tool_calls() != null && !message.tool_calls().isEmpty();
  }

  public Message executeToolCallLoop(List<Message> conversation, List<Tool> tools,
      ToolExecutor executor) {
    Message assistantMsg = sendRequest(conversation, tools);

    int maxIterations = 5;
    int iteration = 0;

    while (hasToolCalls(assistantMsg) && iteration < maxIterations) {
      iteration++;

      List<Message> toolMessages = createToolMessages(assistantMsg, executor);
      conversation.add(assistantMsg);
      conversation.addAll(toolMessages);

      assistantMsg = sendRequest(conversation, tools);
    }

    if (iteration >= maxIterations && hasToolCalls(assistantMsg)) {
      logger.warn("Tool call loop exceeded maximum iterations: {}", maxIterations);
    }

    return assistantMsg;
  }

  private List<Message> createToolMessages(Message assistantMessage, ToolExecutor executor) {
    return assistantMessage.tool_calls().stream()
        .map(toolCall -> {
          String toolResult = executeSingleToolCall(toolCall, executor);
          return Message.tool(toolResult, toolCall.id());
        })
        .toList();
  }

  private String executeSingleToolCall(com.kryo.agents.models.openai.ToolCall toolCall,
      ToolExecutor executor) {
    logger.debug("Executing tool call: {}", toolCall.function().name());
    try {
      return executor.execute(toolCall.function().name(), toolCall.function().arguments());
    } catch (Exception e) {
      logger.error("Tool execution failed: tool={}, error={}", toolCall.function().name(), e.getMessage());
      return String.format("{\"error\": \"%s\"}", e.getMessage());
    }
  }

  @FunctionalInterface
  public interface ToolExecutor {
    String execute(String toolName, String argumentsJson);
  }
}
