package com.kryo.agents;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "azure.openai.endpoint=https://mock.openai.azure.com/",
    "azure.openai.key=mock-key",
    "azure.openai.deployment-name=mock-deployment",
    "azure.openai.api-version=2024-02-15-preview"
})
class AgentsApplicationTests {

  @Test
  void contextLoads() {
  }

}
