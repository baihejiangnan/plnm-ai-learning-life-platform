package com.example.controller;

import com.example.entity.Tag;
import com.example.service.TagService;
import com.example.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import com.example.utils.JwtUtil;
import org.springframework.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 标签控制器
 */
@RestController
@RequestMapping("/api/tags")
@CrossOrigin
public class TagController {

    @Autowired
    private TagService tagService;

    // 新增：用于从JWT中解析userId
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建标签
     */
    @PostMapping
    public Result createTag(@RequestBody Tag tag, HttpServletRequest request) {
        try {
            // 从JWT token中获取当前用户ID
            String token = getTokenFromRequest(request);
            if (token != null && jwtUtil.validateToken(token)) {
                Integer userId = jwtUtil.getUserIdFromToken(token);
                tag.setUserId(userId);
            } else {
                return Result.error("用户未登录或token无效");
            }

            Tag createdTag = tagService.createTag(tag);
            return Result.success(createdTag);
        } catch (Exception e) {
            return Result.error("创建标签失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建标签
     */
    @PostMapping("/batch")
    public Result createTags(@RequestBody List<Tag> tags, HttpServletRequest request) {
        try {
            // 从JWT token中获取当前用户ID
            String token = getTokenFromRequest(request);
            if (token != null && jwtUtil.validateToken(token)) {
                Integer userId = jwtUtil.getUserIdFromToken(token);
                for (Tag t : tags) {
                    t.setUserId(userId);
                }
            } else {
                return Result.error("用户未登录或token无效");
            }

            List<Tag> createdTags = tagService.createTags(tags);
            return Result.success(createdTags);
        } catch (Exception e) {
            return Result.error("批量创建标签失败: " + e.getMessage());
        }
    }

    /**
     * 更新标签
     */
    @PutMapping("/{id}")
    public Result updateTag(@PathVariable Integer id, @RequestBody Tag tag, HttpServletRequest request) {
        try {
            tag.setId(id);
            // 确保userId来自当前登录用户
            String token = getTokenFromRequest(request);
            if (token != null && jwtUtil.validateToken(token)) {
                Integer userId = jwtUtil.getUserIdFromToken(token);
                tag.setUserId(userId);
            } else {
                return Result.error("用户未登录或token无效");
            }

            Tag updatedTag = tagService.updateTag(tag);
            return Result.success(updatedTag);
        } catch (Exception e) {
            return Result.error("更新标签失败: " + e.getMessage());
        }
    }

    /**
     * 删除标签
     */
    @DeleteMapping("/{id}")
    public Result deleteTag(@PathVariable Integer id) {
        try {
            tagService.deleteTag(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除标签失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询标签
     */
    @GetMapping("/{id}")
    public Result getTagById(@PathVariable Integer id) {
        try {
            Tag tag = tagService.getTagById(id);
            if (tag != null) {
                return Result.success(tag);
            } else {
                return Result.error("标签不存在");
            }
        } catch (Exception e) {
            return Result.error("查询标签失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的所有标签
     */
    @GetMapping
    public Result getTagsByUserId(@RequestParam Integer userId) {
        try {
            List<Tag> tags = tagService.getTagsByUserId(userId);
            return Result.success(tags);
        } catch (Exception e) {
            return Result.error("查询标签列表失败: " + e.getMessage());
        }
    }

    /**
     * 根据笔记ID查询关联的标签
     */
    @GetMapping("/note/{noteId}")
    public Result getTagsByNoteId(@PathVariable Long noteId) {
        try {
            List<Tag> tags = tagService.getTagsByNoteId(noteId);
            return Result.success(tags);
        } catch (Exception e) {
            return Result.error("查询笔记标签失败: " + e.getMessage());
        }
    }

    /**
     * 搜索标签
     */
    @GetMapping("/search")
    public Result searchTags(
            @RequestParam Integer userId,
            @RequestParam String keyword) {
        try {
            List<Tag> tags = tagService.searchTags(userId, keyword);
            return Result.success(tags);
        } catch (Exception e) {
            return Result.error("搜索标签失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门标签
     */
    @GetMapping("/popular")
    public Result getPopularTags(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<Tag> popularTags = tagService.getPopularTags(userId, limit);
            return Result.success(popularTags);
        } catch (Exception e) {
            return Result.error("获取热门标签失败: " + e.getMessage());
        }
    }

    /**
     * 获取未使用的标签
     */
    @GetMapping("/unused")
    public Result getUnusedTags(@RequestParam Integer userId) {
        try {
            List<Tag> unusedTags = tagService.getUnusedTags(userId);
            return Result.success(unusedTags);
        } catch (Exception e) {
            return Result.error("获取未使用标签失败: " + e.getMessage());
        }
    }

    /**
     * 智能推荐标签
     */
    @PostMapping("/recommend")
    public Result recommendTags(
            @RequestParam Integer userId,
            @RequestBody Map<String, String> request,
            @RequestParam(defaultValue = "5") Integer limit) {
        try {
            String content = request.get("content");
            List<Tag> recommendedTags = tagService.recommendTags(userId, content, limit);
            return Result.success(recommendedTags);
        } catch (Exception e) {
            return Result.error("推荐标签失败: " + e.getMessage());
        }
    }

    /**
     * 获取或创建标签
     */
    @PostMapping("/get-or-create")
    public Result getOrCreateTag(
            @RequestParam Integer userId,
            @RequestParam String tagName,
            @RequestParam(required = false) String color) {
        try {
            Tag tag = tagService.getOrCreateTag(userId, tagName, color);
            return Result.success(tag);
        } catch (Exception e) {
            return Result.error("获取或创建标签失败: " + e.getMessage());
        }
    }

    /**
     * 批量获取或创建标签
     */
    @PostMapping("/batch-get-or-create")
    public Result getOrCreateTags(
            @RequestParam Integer userId,
            @RequestBody List<String> tagNames) {
        try {
            List<Tag> tags = tagService.getOrCreateTags(userId, tagNames);
            return Result.success(tags);
        } catch (Exception e) {
            return Result.error("批量获取或创建标签失败: " + e.getMessage());
        }
    }

    /**
     * 合并标签
     */
    @PostMapping("/merge")
    public Result mergeTags(
            @RequestParam Integer sourceTagId,
            @RequestParam Integer targetTagId) {
        try {
            tagService.mergeTags(sourceTagId, targetTagId);
            return Result.success("合并成功");
        } catch (Exception e) {
            return Result.error("合并标签失败: " + e.getMessage());
        }
    }

    /**
     * 重命名标签
     */
    @PutMapping("/{id}/rename")
    public Result renameTag(
            @PathVariable Integer id,
            @RequestParam String newName) {
        try {
            Tag tag = tagService.renameTag(id, newName);
            return Result.success(tag);
        } catch (Exception e) {
            return Result.error("重命名标签失败: " + e.getMessage());
        }
    }

    /**
     * 删除未使用的标签
     */
    @DeleteMapping("/unused")
    public Result deleteUnusedTags(@RequestParam Integer userId) {
        try {
            int deletedCount = tagService.deleteUnusedTags(userId);
            return Result.success("删除了 " + deletedCount + " 个未使用的标签");
        } catch (Exception e) {
            return Result.error("删除未使用标签失败: " + e.getMessage());
        }
    }

    /**
     * 统计用户标签数量
     */
    @GetMapping("/count")
    public Result countUserTags(@RequestParam Integer userId) {
        try {
            int count = tagService.countUserTags(userId);
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("统计标签数量失败: " + e.getMessage());
        }
    }

    /**
     * 获取标签云数据
     */
    @GetMapping("/cloud")
    public Result getTagCloud(@RequestParam Integer userId) {
        try {
            List<Map<String, Object>> tagCloud = tagService.getTagCloud(userId);
            return Result.success(tagCloud);
        } catch (Exception e) {
            return Result.error("获取标签云失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建默认标签
     */
    @PostMapping("/init-default")
    public Result initDefaultTags(@RequestParam Integer userId) {
        try {
            // 创建一些默认标签
            String[] defaultTags = {
                "重要", "紧急", "学习", "工作", "生活", 
                "技术", "思考", "总结", "计划", "想法",
                "Java", "Spring", "Vue", "数据库", "算法"
            };
            String[] colors = {
                "#f5222d", "#fa541c", "#1890ff", "#52c41a", "#faad14",
                "#722ed1", "#eb2f96", "#13c2c2", "#a0d911", "#2f54eb",
                "#fa8c16", "#52c41a", "#1890ff", "#722ed1", "#f5222d"
            };
            
            List<Tag> tags = new java.util.ArrayList<>();
            for (int i = 0; i < defaultTags.length; i++) {
                Tag tag = new Tag();
                tag.setUserId(userId);
                tag.setName(defaultTags[i]);
                tag.setColor(colors[i]);
                tag.setDescription("默认标签");
                tags.add(tag);
            }
            
            List<Tag> createdTags = tagService.createTags(tags);
            return Result.success("初始化了 " + createdTags.size() + " 个默认标签");
        } catch (Exception e) {
            return Result.error("初始化默认标签失败: " + e.getMessage());
        }
    }

    /**
     * 从请求中获取Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}