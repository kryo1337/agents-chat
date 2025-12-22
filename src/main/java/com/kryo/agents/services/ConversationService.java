package com.kryo.agents.services;

import com.kryo.agents.models.ChatMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ConversationService {

    private final Map<String, List<ChatMessage>> conversations = new ConcurrentHashMap<>();

    public List<ChatMessage> getHistory(String conversationId) {
        return conversations.computeIfAbsent(conversationId, id -> new CopyOnWriteArrayList<>());
    }

    public void addMessage(String conversationId, ChatMessage message) {
        getHistory(conversationId).add(message);
    }
}
