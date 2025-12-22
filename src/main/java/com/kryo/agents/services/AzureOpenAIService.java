package com.kryo.agents.services;

import com.kryo.agents.models.openai.Message;
import com.kryo.agents.models.openai.OpenAIRequest;
import com.kryo.agents.models.openai.OpenAIResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class AzureOpenAIService {

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

    String result = sendRequest(messages);
    return result != null ? result : "router";
  }

  public String chat(List<com.kryo.agents.models.ChatMessage> history) {
    List<Message> messages = history.stream()
        .map(msg -> new Message(msg.role().name().toLowerCase(), msg.content()))
        .toList();

    String result = sendRequest(messages);
    return result != null ? result : "I apologize, but I am currently unavailable.";
  }

  private String sendRequest(List<Message> messages) {
    OpenAIRequest request = new OpenAIRequest(messages);

    try {
      OpenAIResponse response = restClient.post()
          .uri("/openai/deployments/{deployment}/chat/completions?api-version={version}",
              deploymentName, apiVersion)
          .body(request)
          .retrieve()
          .body(OpenAIResponse.class);

      if (response != null && !response.choices().isEmpty()) {
        return response.choices().get(0).message().content().trim();
      }
    } catch (Exception e) {
      System.err.println("OpenAI API Error: " + e.getMessage());
      return null;
    }
    return null;
  }
}
