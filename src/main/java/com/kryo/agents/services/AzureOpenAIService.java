package com.kryo.agents.services;

import com.kryo.agents.exceptions.AiCallException;
import com.kryo.agents.models.openai.Message;
import com.kryo.agents.models.openai.OpenAIRequest;
import com.kryo.agents.models.openai.OpenAIResponse;
import com.kryo.agents.models.openai.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
    String systemPrompt = """
        You are a helpful support routing assistant.
        Classify the user's message into one of these categories:
        - "technical": for API errors, integration issues, setup, bugs.
        - "billing": for invoices, refunds, payments, subscription plans.
        - "router": if it's general chitchat or unclear.

        Return ONLY the category name in lowercase. Do not add punctuation.
        """;

    List<Message> messages = List.of(
        Message.system(systemPrompt),
        Message.user(userMessage));

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
            String errorBody = new String(resp.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            logger.error("OpenAI API Error: Status={}, Body={}", resp.getStatusCode(), errorBody);
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
}
