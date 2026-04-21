package com.example.controller;

import com.example.common.Result;
import com.example.service.AiAuditLogService;
import com.example.utils.JwtUtil;
import com.github.pagehelper.PageInfo;
import com.example.entity.AiAuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/ai/audit")
public class AiAuditLogController {

    @Autowired
    private AiAuditLogService aiAuditLogService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public Result list(@RequestHeader(value = "Authorization", required = false) String auth,
                       @RequestParam(required = false) String roleId,
                       @RequestParam(required = false) Integer success,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String startTime,
                       @RequestParam(required = false) String endTime,
                       @RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10") Integer size) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        LocalDateTime start = parseTime(startTime);
        LocalDateTime end = parseTime(endTime);
        PageInfo<AiAuditLog> pageInfo = aiAuditLogService.list(userId, roleId, success, keyword, start, end, page, size);
        return Result.success(pageInfo);
    }

    private Integer parseUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.getUserIdFromToken(token);
    }

    private LocalDateTime parseTime(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDateTime.parse(text, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
