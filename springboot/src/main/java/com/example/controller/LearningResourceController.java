package com.example.controller;

import com.example.common.Result;
import com.example.entity.LearningResource;
import com.example.service.LearningResourceService;
import com.example.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning/resources")
public class LearningResourceController {
    @Autowired
    private LearningResourceService service;
    @Autowired
    private JwtUtil jwtUtil;

    private Integer parseUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) return null;
        return jwtUtil.getUserIdFromToken(token);
    }

    @PostMapping
    public Result create(@RequestHeader(value = "Authorization", required = false) String auth,
                         @RequestBody LearningResource r) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        r.setUserId(userId);
        LearningResource created = service.create(r);
        return Result.success(created);
    }

    @PutMapping("/{id}")
    public Result update(@RequestHeader(value = "Authorization", required = false) String auth,
                         @PathVariable Integer id,
                         @RequestBody LearningResource r) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        r.setId(id);
        r.setUserId(userId);
        LearningResource updated = service.update(r);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result delete(@RequestHeader(value = "Authorization", required = false) String auth,
                         @PathVariable Integer id) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        boolean ok = service.delete(id, userId);
        return ok ? Result.success("删除成功") : Result.error("删除失败");
    }

    @GetMapping
    public Result list(@RequestHeader(value = "Authorization", required = false) String auth,
                       @RequestParam(value = "type", required = false) String type,
                       @RequestParam(value = "categoryId", required = false) Integer categoryId) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        List<LearningResource> list;
        if (type != null && !type.isEmpty()) list = service.listByType(userId, type);
        else if (categoryId != null) list = service.listByCategory(userId, categoryId);
        else list = service.list(userId);
        return Result.success(list);
    }
}