package com.example.controller;

import com.example.common.Result;
import com.example.entity.LearningCourseCategory;
import com.example.service.LearningCourseCategoryService;
import com.example.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learning/course-categories")
public class LearningCourseCategoryController {
    @Autowired
    private LearningCourseCategoryService service;
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
                         @RequestBody LearningCourseCategory c) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        c.setUserId(userId);
        if (c.getSortOrder() == null) c.setSortOrder(0);
        LearningCourseCategory created = service.create(c);
        return Result.success(created);
    }

    @PutMapping("/{id}")
    public Result update(@RequestHeader(value = "Authorization", required = false) String auth,
                         @PathVariable Integer id,
                         @RequestBody LearningCourseCategory c) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        c.setId(id);
        c.setUserId(userId);
        LearningCourseCategory updated = service.update(c);
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
    public Result list(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        List<LearningCourseCategory> list = service.list(userId);
        return Result.success(list);
    }

    @PostMapping("/init-default")
    public Result initDefault(@RequestHeader(value = "Authorization", required = false) String auth) {
        Integer userId = parseUserId(auth);
        if (userId == null) return Result.error("未授权访问，请先登录");
        List<LearningCourseCategory> created = service.initDefault(userId);
        return Result.success(created);
    }
}