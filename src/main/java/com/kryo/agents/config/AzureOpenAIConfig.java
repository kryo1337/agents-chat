package com.kryo.agents.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AzureOpenAIConfig {

  @Value("${azure.openai.endpoint}")
  private String endpoint;

  @Value("${azure.openai.key}")
  private String apiKey;

  @Bean
  public RestClient azureOpenAiClient() {
    var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(5000);
    factory.setReadTimeout(10000);

    return RestClient.builder()
        .requestFactory(factory)
        .baseUrl(endpoint)
        .defaultHeader("api-key", apiKey)
        .defaultHeader("Content-Type", "application/json")
        .build();
  }
}
