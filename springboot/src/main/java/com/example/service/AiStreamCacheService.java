package com.example.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiStreamCacheService {

    private final Map<String, StreamCache> cache = new LinkedHashMap<>(128, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, StreamCache> eldest) {
            return size() > 200;
        }
    };

    public synchronized void append(String traceId, Integer userId, String chunk) {
        if (traceId == null) return;
        StreamCache item = cache.get(traceId);
        if (item == null) {
            item = new StreamCache();
            item.userId = userId;
            cache.put(traceId, item);
        }
        if (item.userId == null) {
            item.userId = userId;
        }
        item.content.append(chunk);
        item.updatedAt = LocalDateTime.now();
    }

    public synchronized String getFromOffset(String traceId, Integer userId, int offset) {
        StreamCache item = cache.get(traceId);
        if (item == null) return null;
        if (userId != null && item.userId != null && !userId.equals(item.userId)) return null;
        String text = item.content.toString();
        if (offset < 0) offset = 0;
        if (offset >= text.length()) return "";
        return text.substring(offset);
    }

    public synchronized int length(String traceId) {
        StreamCache item = cache.get(traceId);
        if (item == null) return 0;
        return item.content.length();
    }

    private static class StreamCache {
        private final StringBuilder content = new StringBuilder();
        private LocalDateTime updatedAt = LocalDateTime.now();
        private Integer userId;
    }
}
