package com.example.controller;

import com.example.common.Result;
import com.example.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知管理", description = "站内通知相关接口")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @Operation(summary = "通知列表")
    public Result list(@RequestParam Integer userId,
                       @RequestParam(required = false, defaultValue = "unread") String status,
                       @RequestParam(required = false, defaultValue = "1") Integer page,
                       @RequestParam(required = false, defaultValue = "10") Integer size) {
        try {
            Map<String, Object> data = notificationService.list(userId, status, page, size);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("获取通知列表失败: " + e.getMessage());
        }
    }

    @PostMapping("/read")
    @Operation(summary = "标记为已读")
    public Result markRead(@RequestBody Map<String, Object> body) {
        try {
            Integer userId = (Integer) body.get("userId");
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");
            if (userId == null || ids == null) return Result.error("参数错误");
            boolean ok = notificationService.markRead(userId, ids);
            return ok ? Result.success(true) : Result.error("标记失败");
        } catch (Exception e) {
            return Result.error("标记异常: " + e.getMessage());
        }
    }

    @DeleteMapping
    @Operation(summary = "删除通知")
    public Result delete(@RequestBody Map<String, Object> body) {
        try {
            Integer userId = (Integer) body.get("userId");
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) body.get("ids");
            if (userId == null || ids == null) return Result.error("参数错误");
            boolean ok = notificationService.delete(userId, ids);
            return ok ? Result.success(true) : Result.error("删除失败");
        } catch (Exception e) {
            return Result.error("删除异常: " + e.getMessage());
        }
    }

    @GetMapping("/settings")
    @Operation(summary = "获取通知设置")
    public Result getSettings(@RequestParam Integer userId) {
        try {
            Map<String, Object> s = notificationService.getSettings(userId);
            return Result.success(s);
        } catch (Exception e) {
            return Result.error("获取设置失败: " + e.getMessage());
        }
    }

    @PutMapping("/settings")
    @Operation(summary = "更新通知设置")
    public Result updateSettings(@RequestParam Integer userId, @RequestBody Map<String, Object> payload) {
        try {
            boolean ok = notificationService.updateSettings(userId, payload);
            return ok ? Result.success("保存成功") : Result.error("保存失败");
        } catch (Exception e) {
            return Result.error("保存异常: " + e.getMessage());
        }
    }
}