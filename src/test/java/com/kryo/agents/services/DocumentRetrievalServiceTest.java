package com.kryo.agents.services;

import com.kryo.agents.models.DocumentChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentRetrievalServiceTest {

  private DocumentRetrievalService retrievalService;

  @BeforeEach
  void setUp() {
    retrievalService = new DocumentRetrievalService();
    retrievalService.init();
  }

  @Test
  void retrieveDocuments_exactKeyword_findsRelevantChunk() {
    List<DocumentChunk> results = retrievalService.retrieveDocuments("503 error");

    assertFalse(results.isEmpty(), "Should find documents for '503 error'");

    boolean foundTroubleshooting = results.stream()
        .anyMatch(chunk -> chunk.source().contains("troubleshooting.md")
            && chunk.content().contains("503"));

    assertTrue(foundTroubleshooting, "Should retrieve troubleshooting guide for 503 error");
  }

  @Test
  void retrieveDocuments_apiKey_findsIntegrationGuide() {
    List<DocumentChunk> results = retrievalService.retrieveDocuments("api key configuration");

    assertFalse(results.isEmpty());

    boolean foundAuth = results.stream()
        .anyMatch(chunk -> chunk.source().contains("api-integration-guide.md")
            || chunk.content().toLowerCase().contains("api key"));

    assertTrue(foundAuth, "Should find content related to API keys");
  }

  @Test
  void retrieveDocuments_irrelevantQuery_returnsEmptyOrLowScore() {
    List<DocumentChunk> results = retrievalService.retrieveDocuments("potato salad recipe");

    assertTrue(results.size() <= 1, "Should return few or no results for irrelevant query");
  }

  @Test
  void retrieveDocuments_emptyQuery_returnsEmptyList() {
    List<DocumentChunk> results = retrievalService.retrieveDocuments("");
    assertTrue(results.isEmpty());
  }
}
