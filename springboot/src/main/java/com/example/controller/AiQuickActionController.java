package com.example.controller;

import com.example.common.Result;
import com.example.service.AiQuickActionService;
import com.example.service.AiStreamService;
import com.example.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiQuickActionController {

    @Autowired
    private AiQuickActionService aiQuickActionService;

    @Autowired
    private AiStreamService aiStreamService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/quick-action")
    public Result quickAction(@RequestHeader(value = "Authorization", required = false) String auth,
                              @RequestBody Map<String, Object> request) {
        Integer userId = parseUserId(auth);
        if (userId == null) {
            return Result.error("未授权访问，请先登录");
        }
        Object messageValue = request == null ? null : request.get("message");
        String message = messageValue == null ? "" : String.valueOf(messageValue);
        Map<String, Object> result = aiQuickActionService.handle(userId, message);
        return Result.success(result);
    }

    @GetMapping("/roles")
    public Result roles(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer userId = parseUserId(auth);
        if (userId == null) {
            return Result.error("未授权访问，请先登录");
        }
        return Result.success(aiQuickActionService.listRoles());
    }

    @PostMapping("/chat")
    public Result chat(@RequestHeader(value = "Authorization", required = false) String auth,
                       @RequestBody Map<String, Object> request) {
        Integer userId = parseUserId(auth);
        if (userId == null) {
            return Result.error("未授权访问，请先登录");
        }
        String message = request == null || request.get("message") == null ? "" : String.valueOf(request.get("message"));
        String roleId = request == null || request.get("roleId") == null ? "system_operator" : String.valueOf(request.get("roleId"));
        String crossRoleContext = request == null || request.get("crossRoleContext") == null ? null : String.valueOf(request.get("crossRoleContext"));
        boolean allowCrossRoleReference = request != null && Boolean.parseBoolean(String.valueOf(request.getOrDefault("allowCrossRoleReference", false)));
        return Result.success(aiQuickActionService.handle(userId, roleId, message, crossRoleContext, allowCrossRoleReference));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestHeader(value = "Authorization", required = false) String auth,
                             @RequestParam(value = "token", required = false) String tokenParam,
                             @RequestParam(value = "roleId", required = false, defaultValue = "system_operator") String roleId,
                             @RequestParam(value = "message") String message,
                             @RequestParam(value = "crossRoleContext", required = false) String crossRoleContext,
                             @RequestParam(value = "allowCrossRoleReference", required = false, defaultValue = "false") boolean allowCrossRoleReference) {
        Integer userId = parseUserId(auth);
        if (userId == null && tokenParam != null) {
            userId = parseUserId("Bearer " + tokenParam);
        }
        if (userId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data("未授权访问，请先登录"));
            } catch (Exception ignored) {
            } finally {
                emitter.complete();
            }
            return emitter;
        }
        return aiStreamService.stream(userId, roleId, message, crossRoleContext, allowCrossRoleReference);
    }

    @GetMapping(value = "/stream/resume", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter resume(@RequestHeader(value = "Authorization", required = false) String auth,
                             @RequestParam(value = "traceId") String traceId,
                             @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        Integer userId = parseUserId(auth);
        if (userId == null) {
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event().name("error").data("未授权访问，请先登录"));
            } catch (Exception ignored) {
            } finally {
                emitter.complete();
            }
            return emitter;
        }
        return aiStreamService.resume(userId, traceId, offset == null ? 0 : offset);
    }

    private Integer parseUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.getUserIdFromToken(token);
    }
}
