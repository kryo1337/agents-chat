package com.kryo.agents.services;

import com.kryo.agents.models.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ConversationService {

  private static final int MAX_CONVERSATIONS = 100;

  private final Map<String, List<ChatMessage>> conversations = Collections.synchronizedMap(
      new LinkedHashMap<String, List<ChatMessage>>(MAX_CONVERSATIONS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<ChatMessage>> eldest) {
          return size() > MAX_CONVERSATIONS;
        }
      });

  public List<ChatMessage> getHistory(String conversationId) {
    return conversations.computeIfAbsent(conversationId, id -> new CopyOnWriteArrayList<>());
  }

  public void addMessage(String conversationId, ChatMessage message) {
    getHistory(conversationId).add(message);
  }

  public List<ChatMessage> getRecentHistory(String conversationId, int maxMessages) {
    List<ChatMessage> fullHistory = getHistory(conversationId);
    if (fullHistory.size() <= maxMessages) {
      return List.copyOf(fullHistory);
    }
    return List.copyOf(fullHistory.subList(fullHistory.size() - maxMessages, fullHistory.size()));
  }
}
