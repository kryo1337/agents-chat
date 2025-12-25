package com.kryo.agents.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kryo.agents.agents.Agent;
import com.kryo.agents.agents.AgentOrchestrator;
import com.kryo.agents.agents.BillingAgent;
import com.kryo.agents.agents.RouterAgent;
import com.kryo.agents.agents.TechnicalAgent;
import com.kryo.agents.services.AzureOpenAIService;
import com.kryo.agents.services.BillingService;
import com.kryo.agents.services.ConversationService;
import com.kryo.agents.services.DocumentRetrievalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentSwitchingTest {

  private AgentOrchestrator orchestrator;

  @Mock
  private AzureOpenAIService openAIService;

  @Mock
  private DocumentRetrievalService documentRetrievalService;

  @Mock
  private BillingService billingService;

  private ConversationService conversationService;

  @BeforeEach
  void setUp() {
    ObjectMapper objectMapper = new ObjectMapper();
    conversationService = new ConversationService();

    BillingAgent billingAgent = new BillingAgent(openAIService, billingService, objectMapper);
    TechnicalAgent technicalAgent = new TechnicalAgent(openAIService, documentRetrievalService);
    RouterAgent routerAgent = new RouterAgent();

    orchestrator = new AgentOrchestrator(
        List.of(billingAgent, technicalAgent, routerAgent),
        openAIService,
        conversationService);
  }

  @Test
  void singleConversation_pureBilling_usesBillingAgent() {
    when(openAIService.classifyIntent(anyList())).thenReturn("billing");

    String conversationId = "test-billing-only";

    Agent agent1 = orchestrator.route(conversationId, "What is my subscription status?");
    assertEquals("billing", agent1.getName());

    Agent agent2 = orchestrator.route(conversationId, "I want a refund for my plan");
    assertEquals("billing", agent2.getName());
  }

  @Test
  void singleConversation_pureTechnical_usesTechnicalAgent() {
    when(openAIService.classifyIntent(anyList())).thenReturn("technical");

    String conversationId = "test-technical-only";

    Agent agent1 = orchestrator.route(conversationId, "How do I authenticate the API?");
    assertEquals("technical", agent1.getName());

    Agent agent2 = orchestrator.route(conversationId, "I'm getting a 401 error");
    assertEquals("technical", agent2.getName());
  }

  @Test
  void conversation_switchToBilling_billingAgentTakesOver() {
    String conversationId = "test-to-billing";

    when(openAIService.classifyIntent(anyList())).thenReturn("technical", "billing");

    Agent agent1 = orchestrator.route(conversationId, "How do I set up API keys?");
    assertEquals("technical", agent1.getName());

    Agent agent2 = orchestrator.route(conversationId, "Can I get a refund for my Pro plan?");
    assertEquals("billing", agent2.getName());
  }

  @Test
  void conversation_switchToTechnical_technicalAgentTakesOver() {
    String conversationId = "test-to-technical";

    when(openAIService.classifyIntent(anyList())).thenReturn("billing", "technical");

    Agent agent1 = orchestrator.route(conversationId, "What is my plan price?");
    assertEquals("billing", agent1.getName());

    Agent agent2 = orchestrator.route(conversationId, "I'm getting a 401 error when calling the API");
    assertEquals("technical", agent2.getName());
  }

  @Test
  void conversation_multipleSwitches_switchesCorrectly() {
    String conversationId = "test-multiple-switches";

    when(openAIService.classifyIntent(anyList()))
        .thenReturn("technical", "billing", "technical", "billing");

    Agent agent1 = orchestrator.route(conversationId, "How do I run the app locally?");
    assertEquals("technical", agent1.getName());

    Agent agent2 = orchestrator.route(conversationId, "Check my subscription");
    assertEquals("billing", agent2.getName());

    Agent agent3 = orchestrator.route(conversationId, "What's the main API endpoint?");
    assertEquals("technical", agent3.getName());

    Agent agent4 = orchestrator.route(conversationId, "Can you explain the refund policy?");
    assertEquals("billing", agent4.getName());
  }

  @Test
  void conversation_routerSuggestion_keepsCurrentAgent() {
    String conversationId = "test-router-suggestion";

    when(openAIService.classifyIntent(anyList()))
        .thenReturn("billing", "router", "billing");

    Agent agent1 = orchestrator.route(conversationId, "What is my plan?");
    assertEquals("billing", agent1.getName());

    Agent agent2 = orchestrator.route(conversationId, "What's the weather?");
    assertEquals("billing", agent2.getName(), "Should keep current agent when router is suggested");

    Agent agent3 = orchestrator.route(conversationId, "I want to change my plan");
    assertEquals("billing", agent3.getName());
  }

  @Test
  void newConversation_noHistory_usesSuggestedAgent() {
    when(openAIService.classifyIntent(anyList())).thenReturn("technical");

    Agent agent = orchestrator.route("new-conversation", "How do I authenticate?");
    assertEquals("technical", agent.getName());
  }

  @Test
  void nullConversationId_routesCorrectly() {
    when(openAIService.classifyIntent(anyList())).thenReturn("billing");

    Agent agent = orchestrator.route(null, "Check my subscription");
    assertEquals("billing", agent.getName());
  }

  @Test
  void emptyMessage_routesToRouter() {
    Agent agent = orchestrator.route("test-empty", "");
    assertEquals("router", agent.getName());
  }

  @Test
  void nullMessage_routesToRouter() {
    Agent agent = orchestrator.route("test-null", null);
    assertEquals("router", agent.getName());
  }

  @Test
  void clearConversationAgent_resetsTracking() {
    when(openAIService.classifyIntent(anyList())).thenReturn("billing");

    String conversationId = "test-clear";

    Agent agent1 = orchestrator.route(conversationId, "What's my plan?");
    assertEquals("billing", agent1.getName());

    orchestrator.clearConversationAgent(conversationId);

    when(openAIService.classifyIntent(anyList())).thenReturn("technical");

    Agent agent2 = orchestrator.route(conversationId, "How do I authenticate?");
    assertEquals("technical", agent2.getName(), "Should route based on suggestion after clearing");
  }

  @Test
  void differentConversations_independentAgents() {
    when(openAIService.classifyIntent(anyList())).thenReturn("technical", "billing", "technical", "billing");

    String conv1 = "conv-1";
    String conv2 = "conv-2";

    Agent agent1 = orchestrator.route(conv1, "How do I authenticate?");
    assertEquals("technical", agent1.getName());

    Agent agent2 = orchestrator.route(conv2, "What's my plan price?");
    assertEquals("billing", agent2.getName());

    Agent agent1b = orchestrator.route(conv1, "I'm getting a 429 error");
    assertEquals("technical", agent1b.getName(), "Conversation 1 should still use technical agent");

    Agent agent2b = orchestrator.route(conv2, "I want a refund");
    assertEquals("billing", agent2b.getName(), "Conversation 2 should still use billing agent");
  }
}
