package com.kryo.agents.services;

import com.kryo.agents.config.AppConstants;
import com.kryo.agents.models.ChatMessage;
import com.kryo.agents.models.ConversationSummary;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ConversationService {

  private final LinkedHashMap<String, List<ChatMessage>> conversations = new LinkedHashMap<String, List<ChatMessage>>(
      AppConstants.MAX_TRACKED_CONVERSATIONS, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, List<ChatMessage>> eldest) {
      if (size() > AppConstants.MAX_TRACKED_CONVERSATIONS) {
        titles.remove(eldest.getKey());
        conversationOwners.remove(eldest.getKey());
        return true;
      }
      return false;
    }
  };

  private final Map<String, String> titles = Collections.synchronizedMap(new LinkedHashMap<>());
  private final Map<String, String> conversationOwners = new java.util.concurrent.ConcurrentHashMap<>();

  public List<ChatMessage> getHistory(String conversationId) {
    synchronized (conversations) {
      return conversations.computeIfAbsent(conversationId, id -> new CopyOnWriteArrayList<>());
    }
  }

  public java.util.Optional<List<ChatMessage>> findHistory(String conversationId, String userId) {
    if (!userId.equals(conversationOwners.get(conversationId))) {
      return java.util.Optional.empty();
    }
    synchronized (conversations) {
      return java.util.Optional.ofNullable(conversations.get(conversationId));
    }
  }

  public void addMessage(String conversationId, String userId, ChatMessage message) {
    conversationOwners.putIfAbsent(conversationId, userId);

    List<ChatMessage> history = getHistory(conversationId);
    history.add(message);

    if (message.role() == com.kryo.agents.models.Role.USER && !titles.containsKey(conversationId)) {
      String content = message.content();
      String title = content.length() > 30 ? content.substring(0, 27) + "..." : content;
      titles.put(conversationId, title);
    }
  }

  public List<ChatMessage> getRecentHistory(String conversationId, int maxMessages) {
    List<ChatMessage> fullHistory = getHistory(conversationId);
    int size = fullHistory.size();
    if (size <= maxMessages) {
      return List.copyOf(fullHistory);
    }
    return List.copyOf(fullHistory.subList(size - maxMessages, size));
  }

  public List<ConversationSummary> getSummaries(String userId) {
    List<ConversationSummary> summaries = new ArrayList<>();
    synchronized (conversations) {
      for (String id : conversations.sequencedKeySet().reversed()) {
        if (userId.equals(conversationOwners.get(id))) {
          String title = titles.getOrDefault(id, "New Conversation");
          summaries.add(new ConversationSummary(id, title, "Just now"));
        }
      }
    }
    return summaries;
  }
}
