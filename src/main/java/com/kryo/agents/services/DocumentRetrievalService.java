package com.kryo.agents.services;

import com.kryo.agents.models.DocumentChunk;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentRetrievalService {

  private final List<DocumentChunk> documentStore = new ArrayList<>();

  @PostConstruct
  public void init() {
    loadDocuments();
  }

  private void loadDocuments() {
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    try {
      Resource[] resources = resolver.getResources("classpath:technical-docs/*.md");
      for (Resource resource : resources) {
        parseAndStore(resource);
      }
      System.out.println("Loaded " + documentStore.size() + " document chunks.");
    } catch (IOException e) {
      System.err.println("Failed to load technical documents: " + e.getMessage());
    }
  }

  private void parseAndStore(Resource resource) throws IOException {
    String filename = resource.getFilename();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      String line;
      String currentHeader = "General";
      StringBuilder currentContent = new StringBuilder();

      while ((line = reader.readLine()) != null) {
        if (line.startsWith("#")) {
          if (!currentContent.isEmpty()) {
            documentStore.add(new DocumentChunk(filename, currentHeader, currentContent.toString().trim()));
            currentContent.setLength(0);
          }
          currentHeader = line.replaceAll("#+", "").trim();
        } else {
          currentContent.append(line).append("\n");
        }
      }
      if (!currentContent.isEmpty()) {
        documentStore.add(new DocumentChunk(filename, currentHeader, currentContent.toString().trim()));
      }
    }
  }

  public List<DocumentChunk> retrieveDocuments(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }

    String processedQuery = normalizeAndStem(query);

    return documentStore.stream()
        .map(chunk -> new ScoredChunk(chunk, calculateScore(chunk, processedQuery)))
        .filter(sc -> sc.score >= 2)
        .sorted(Comparator.comparingInt(ScoredChunk::score).reversed())
        .limit(5)
        .map(ScoredChunk::chunk)
        .collect(Collectors.toList());
  }

  private int calculateScore(DocumentChunk chunk, String processedQuery) {
    int score = 0;
    String content = normalizeAndStem(chunk.header() + " " + chunk.content());
    String[] queryTokens = processedQuery.split("\\s+");

    for (String token : queryTokens) {
      if (token.length() < 3)
        continue;

      if (content.contains(" " + token + " ")) {
        score += 2;
      } else if (content.contains(token)) {
        score += 1;
      }
    }
    return score;
  }

  private String normalizeAndStem(String text) {
    if (text == null)
      return "";
    String normalized = text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");

    StringBuilder stemmed = new StringBuilder();
    for (String word : normalized.split("\\s+")) {
      if (word.endsWith("ing"))
        word = word.substring(0, word.length() - 3);
      else if (word.endsWith("s") && !word.endsWith("ss"))
        word = word.substring(0, word.length() - 1);
      else if (word.endsWith("ed"))
        word = word.substring(0, word.length() - 2);

      stemmed.append(" ").append(word).append(" ");
    }
    return stemmed.toString();
  }

  private record ScoredChunk(DocumentChunk chunk, int score) {
  }
}
