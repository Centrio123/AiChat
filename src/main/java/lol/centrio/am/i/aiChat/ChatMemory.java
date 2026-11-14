package lol.centrio.am.i.aiChat;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ChatMemory {
    private static final Map<UUID, List<ChatMessage>> historyMap = new ConcurrentHashMap<>();

    public static class ChatMessage {
        public String role; // "user" or "assistant"
        public String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    public static void addMessage(UUID uuid, String role, String content, int maxHistory) {
        historyMap.putIfAbsent(uuid, new CopyOnWriteArrayList<>());
        List<ChatMessage> history = historyMap.get(uuid);

        history.add(new ChatMessage(role, content));

        while (history.size() > (maxHistory * 2)) {
            if (!history.isEmpty()) {
                history.remove(0);
            }
        }
    }

    public static List<ChatMessage> getHistory(UUID uuid) {
        return historyMap.getOrDefault(uuid, new ArrayList<>());
    }

    public static void clearHistory(UUID uuid) {
        historyMap.remove(uuid);
    }
}