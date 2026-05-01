package com.example.config;

import com.example.entity.AiAuditLog;
import com.example.service.AiAuditLogService;
import com.example.service.AiQuickActionService;
import com.example.service.AiQuickActionService.RolePrompt;
import com.example.service.DeepSeekChatClient;
import com.example.service.AiRoleConfigService;
import com.example.service.AiStreamCacheService;
import com.example.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AiChatWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private DeepSeekChatClient deepSeekChatClient;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AiRoleConfigService aiRoleConfigService;

    @Autowired
    private AiQuickActionService aiQuickActionService;

    @Autowired
    private AiAuditLogService aiAuditLogService;

    @Autowired
    private AiStreamCacheService aiStreamCacheService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String token = getToken(session);
        Integer userId = validateToken(token);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("unauthorized"));
            return;
        }
        session.getAttributes().put("userId", userId);
        sendEvent(session, "ready", Map.of("ok", true));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId == null) {
            sendEvent(session, "error", Map.of("message", "unauthorized"));
            return;
        }
        Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
        String action = String.valueOf(payload.getOrDefault("action", "chat"));
        if ("resume".equalsIgnoreCase(action)) {
            handleResume(session, payload);
            return;
        }
        handleChat(session, userId, payload);
    }

    private void handleChat(WebSocketSession session, Integer userId, Map<String, Object> payload) throws IOException {
        String roleId = String.valueOf(payload.getOrDefault("roleId", "system_operator"));
        String message = String.valueOf(payload.getOrDefault("message", ""));
        boolean allowCrossRoleReference = Boolean.parseBoolean(String.valueOf(payload.getOrDefault("allowCrossRoleReference", false)));
        String crossRoleContext = String.valueOf(payload.getOrDefault("crossRoleContext", ""));
        String traceId = String.valueOf(payload.getOrDefault("traceId", UUID.randomUUID().toString()));
        sendEvent(session, "session", Map.of("traceId", traceId));

        AiRoleConfigService.AiRole role = aiRoleConfigService.getRole(roleId);
        if (role == null) {
            sendEvent(session, "error", Map.of("message", "role not found"));
            return;
        }
        if (!role.isStreaming()) {
            Map<String, Object> res = aiQuickActionService.handle(userId, roleId, message, crossRoleContext, allowCrossRoleReference);
            sendEvent(session, "meta", Map.of("intent", res.get("intent"), "citations", res.getOrDefault("citations", List.of())));
            sendEvent(session, "chunk", Map.of("data", String.valueOf(res.getOrDefault("reply", ""))));
            sendEvent(session, "done", Map.of("ok", true));
            return;
        }

        RolePrompt prompt = aiQuickActionService.prepareRolePrompt(userId, role, message, allowCrossRoleReference ? crossRoleContext : "");
        if (prompt == null) {
            sendEvent(session, "chunk", Map.of("data", "当前知识库不足以回答。"));
            sendEvent(session, "done", Map.of("ok", true));
            return;
        }
        sendEvent(session, "meta", Map.of("intent", prompt.getIntent(), "citations", prompt.getCitations(), "traceId", traceId));

        if (!deepSeekChatClient.isConfigured()) {
            String fallback = prompt.getFallbackReply();
            aiStreamCacheService.append(traceId, userId, fallback);
            sendEvent(session, "chunk", Map.of("data", fallback));
            sendEvent(session, "done", Map.of("ok", true));
            return;
        }

        long start = System.currentTimeMillis();
        StringBuilder buffer = new StringBuilder();
        Flux<String> flux;
        flux = deepSeekChatClient.stream(prompt.getPrompt(), role.getTemperature(), role.getTopP());

        flux.subscribe(
                chunk -> {
                    buffer.append(chunk);
                    aiStreamCacheService.append(traceId, userId, chunk);
                    sendEvent(session, "chunk", Map.of("data", chunk));
                },
                error -> {
                    recordAudit(userId, role.getId(), prompt.getIntent(), role.getKnowledgeBase(), message, buffer.toString(), traceId, start);
                    sendEvent(session, "done", Map.of("ok", false));
                },
                () -> {
                    recordAudit(userId, role.getId(), prompt.getIntent(), role.getKnowledgeBase(), message, buffer.toString(), traceId, start);
                    sendEvent(session, "done", Map.of("ok", true));
                }
        );
    }

    private void handleResume(WebSocketSession session, Map<String, Object> payload) throws IOException {
        Integer userId = (Integer) session.getAttributes().get("userId");
        String traceId = String.valueOf(payload.get("traceId"));
        int offset = Integer.parseInt(String.valueOf(payload.getOrDefault("offset", 0)));
        String remain = aiStreamCacheService.getFromOffset(traceId, userId, offset);
        if (remain == null) {
            sendEvent(session, "error", Map.of("message", "resume cache missing"));
            return;
        }
        if (!remain.isEmpty()) {
            sendEvent(session, "chunk", Map.of("data", remain));
        }
        sendEvent(session, "done", Map.of("ok", true));
    }

    private String getToken(WebSocketSession session) {
        String query = session.getUri() == null ? "" : session.getUri().getQuery();
        if (!StringUtils.hasText(query)) return null;
        for (String part : query.split("&")) {
            if (part.startsWith("token=")) {
                return part.substring(6);
            }
        }
        return null;
    }

    private Integer validateToken(String token) {
        if (!StringUtils.hasText(token)) return null;
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.getUserIdFromToken(token);
    }

    private void sendEvent(WebSocketSession session, String type, Map<String, Object> data) {
        if (session == null || !session.isOpen()) return;
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("data", data);
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException ignored) {
        }
    }

    private void recordAudit(Integer userId, String roleId, String actionType, String module, String requestText, String responseText, String traceId, long startTime) {
        AiAuditLog log = new AiAuditLog();
        log.setUserId(userId);
        log.setRoleId(roleId);
        log.setActionType(actionType);
        log.setModule(module);
        log.setRequestText(requestText);
        log.setResponseText(responseText);
        log.setSuccess(1);
        log.setLatencyMs(System.currentTimeMillis() - startTime);
        log.setTraceId(traceId);
        log.setCreatedTime(LocalDateTime.now());
        aiAuditLogService.record(log);
    }
}
