package com.example.service;

import com.example.entity.AiAuditLog;
import com.example.service.AiQuickActionService.RolePrompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AiStreamService {

    @Autowired(required = false)
    private OpenAiChatModel openAiChatModel;

    @Autowired
    private AiRoleConfigService aiRoleConfigService;

    @Autowired
    private AiQuickActionService aiQuickActionService;

    @Autowired
    private AiAuditLogService aiAuditLogService;

    @Autowired
    private AiStreamCacheService aiStreamCacheService;

    public SseEmitter stream(Integer userId, String roleId, String message, String crossRoleContext, boolean allowCrossRoleReference) {
        SseEmitter emitter = new SseEmitter(0L);
        String traceId = UUID.randomUUID().toString();
        AiRoleConfigService.AiRole role = aiRoleConfigService.getRole(roleId);
        if (role == null) {
            emitError(emitter, "角色不存在");
            return emitter;
        }

        if (!role.isStreaming()) {
            Map<String, Object> response = aiQuickActionService.handle(userId, roleId, message, crossRoleContext, allowCrossRoleReference);
            sendChunk(emitter, String.valueOf(response.getOrDefault("reply", "")));
            sendMeta(emitter, response.get("intent"), response.get("citations"), traceId);
            complete(emitter);
            return emitter;
        }

        RolePrompt prompt = aiQuickActionService.prepareRolePrompt(userId, role, message, allowCrossRoleReference ? crossRoleContext : "");
        if (prompt == null) {
            sendChunk(emitter, "当前知识库不足以回答。");
            sendMeta(emitter, "EMPTY_CONTEXT", List.of(), traceId);
            complete(emitter);
            return emitter;
        }

        sendMeta(emitter, prompt.getIntent(), prompt.getCitations(), traceId);
        if (openAiChatModel == null) {
            sendChunk(emitter, prompt.getFallbackReply());
            complete(emitter);
            return emitter;
        }
        long start = System.currentTimeMillis();
        StringBuilder buffer = new StringBuilder();
        Flux<String> flux;
        try {
            flux = (Flux<String>) openAiChatModel.getClass().getMethod("stream", String.class).invoke(openAiChatModel, prompt.getPrompt());
        } catch (Exception e) {
            sendChunk(emitter, prompt.getFallbackReply());
            complete(emitter);
            return emitter;
        }

        flux.subscribe(
                chunk -> {
                    buffer.append(chunk);
                    aiStreamCacheService.append(traceId, userId, chunk);
                    sendChunk(emitter, chunk);
                },
                error -> {
                    if (buffer.length() == 0) {
                        sendChunk(emitter, prompt.getFallbackReply());
                    }
                    recordAudit(userId, role.getId(), prompt.getIntent(), role.getKnowledgeBase(), message, buffer.toString(), traceId, start);
                    complete(emitter);
                },
                () -> {
                    recordAudit(userId, role.getId(), prompt.getIntent(), role.getKnowledgeBase(), message, buffer.toString(), traceId, start);
                    complete(emitter);
                }
        );
        return emitter;
    }

    public SseEmitter resume(Integer userId, String traceId, int offset) {
        SseEmitter emitter = new SseEmitter(0L);
        String remain = aiStreamCacheService.getFromOffset(traceId, userId, offset);
        if (remain == null) {
            emitError(emitter, "缓存不存在或已过期");
            return emitter;
        }
        if (!remain.isEmpty()) {
            sendChunk(emitter, remain);
        }
        complete(emitter);
        return emitter;
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        if (!StringUtils.hasText(chunk)) return;
        try {
            emitter.send(SseEmitter.event().name("chunk").data(chunk), MediaType.TEXT_EVENT_STREAM);
        } catch (IOException ignored) {
        }
    }

    private void sendMeta(SseEmitter emitter, Object intent, Object citations, String traceId) {
        try {
            emitter.send(SseEmitter.event().name("meta").data(Map.of(
                    "intent", intent,
                    "citations", citations == null ? List.of() : citations,
                    "traceId", traceId
            )), MediaType.TEXT_EVENT_STREAM);
        } catch (IOException ignored) {
        }
    }

    private void emitError(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(message), MediaType.TEXT_EVENT_STREAM);
        } catch (IOException ignored) {
        } finally {
            emitter.complete();
        }
    }

    private void complete(SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("done").data("[DONE]"), MediaType.TEXT_EVENT_STREAM);
        } catch (IOException ignored) {
        } finally {
            emitter.complete();
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
