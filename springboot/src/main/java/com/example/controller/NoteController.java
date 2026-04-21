package com.example.controller;

import com.example.entity.Note;
import com.example.service.NoteService;
import com.example.common.Result;
import com.example.utils.JwtUtil;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 笔记控制器
 */
@RestController
@RequestMapping("/api/notes")
@CrossOrigin
public class NoteController {

    @Autowired
    private NoteService noteService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 创建笔记
     */
    @PostMapping
    public Result createNote(@RequestBody Note note, HttpServletRequest request) {
        try {
            // 从JWT token中获取当前用户ID
            String token = getTokenFromRequest(request);
            if (token != null && jwtUtil.validateToken(token)) {
                Integer userId = jwtUtil.getUserIdFromToken(token);
                note.setUserId(userId);
            } else {
                return Result.error("用户未登录或token无效");
            }
            
            Note createdNote = noteService.createNote(note);
            return Result.success(createdNote);
        } catch (Exception e) {
            return Result.error("创建笔记失败: " + e.getMessage());
        }
    }

    /**
     * 更新笔记（带ID路径参数）
     */
    @PutMapping("/{id}")
    public Result updateNote(@PathVariable Long id, @RequestBody Note note) {
        try {
            note.setId(id);
            Note updatedNote = noteService.updateNote(note);
            return Result.success(updatedNote);
        } catch (Exception e) {
            return Result.error("更新笔记失败: " + e.getMessage());
        }
    }

    /**
     * 更新笔记（从请求体获取ID）
     */
    @PutMapping
    public Result updateNoteFromBody(@RequestBody Note note, HttpServletRequest request) {
        try {
            // 验证用户权限
            String token = getTokenFromRequest(request);
            if (token != null && jwtUtil.validateToken(token)) {
                Integer userId = jwtUtil.getUserIdFromToken(token);
                // 可以添加权限验证，确保用户只能更新自己的笔记
                note.setUserId(userId);
            } else {
                return Result.error("用户未登录或token无效");
            }
            
            if (note.getId() == null) {
                return Result.error("笔记ID不能为空");
            }
            
            Note updatedNote = noteService.updateNote(note);
            return Result.success(updatedNote);
        } catch (Exception e) {
            return Result.error("更新笔记失败: " + e.getMessage());
        }
    }

    /**
     * 删除笔记（软删除）
     */
    @DeleteMapping("/{id}")
    public Result deleteNote(@PathVariable Long id) {
        try {
            noteService.deleteNote(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除笔记失败: " + e.getMessage());
        }
    }

    /**
     * 彻底删除笔记
     */
    @DeleteMapping("/{id}/permanent")
    public Result permanentDeleteNote(@PathVariable Long id) {
        try {
            noteService.permanentDeleteNote(id);
            return Result.success("彻底删除成功");
        } catch (Exception e) {
            return Result.error("彻底删除笔记失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询笔记
     */
    @GetMapping("/{id}")
    public Result getNoteById(@PathVariable Long id) {
        try {
            Note note = noteService.getNoteById(id);
            if (note != null) {
                // 增加浏览次数
                noteService.incrementViewCount(id);
                return Result.success(note);
            } else {
                return Result.error("笔记不存在");
            }
        } catch (Exception e) {
            return Result.error("查询笔记失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的笔记列表
     */
    @GetMapping
    public Result getNotesByUserId(
            @RequestParam Integer userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Integer tagId) {
        try {
            PageInfo<Note> pageInfo = noteService.getNotesByUserId(userId, status, categoryId, keyword, page, size, tagId);
            return Result.success(pageInfo);
        } catch (Exception e) {
            return Result.error("查询笔记列表失败: " + e.getMessage());
        }
    }

    /**
     * 搜索笔记
     */
    @GetMapping("/search")
    public Result searchNotes(
            @RequestParam Integer userId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        try {
            PageInfo<Note> pageInfo = noteService.searchNotes(userId, keyword, page, size);
            return Result.success(pageInfo);
        } catch (Exception e) {
            return Result.error("搜索笔记失败: " + e.getMessage());
        }
    }

    /**
     * 发布笔记
     */
    @PutMapping("/{id}/publish")
    public Result publishNote(@PathVariable Long id) {
        try {
            noteService.publishNote(id);
            return Result.success("发布成功");
        } catch (Exception e) {
            return Result.error("发布笔记失败: " + e.getMessage());
        }
    }

    /**
     * 归档笔记
     */
    @PutMapping("/{id}/archive")
    public Result archiveNote(@PathVariable Long id) {
        try {
            noteService.archiveNote(id);
            return Result.success("归档成功");
        } catch (Exception e) {
            return Result.error("归档笔记失败: " + e.getMessage());
        }
    }

    /**
     * 恢复笔记
     */
    @PutMapping("/{id}/restore")
    public Result restoreNote(@PathVariable Long id) {
        try {
            noteService.restoreNote(id);
            return Result.success("恢复成功");
        } catch (Exception e) {
            return Result.error("恢复笔记失败: " + e.getMessage());
        }
    }

    /**
     * 获取笔记统计信息
     */
    @GetMapping("/statistics")
    public Result getNoteStatistics(@RequestParam Integer userId) {
        try {
            Map<String, Object> statistics = noteService.getNoteStatistics(userId);
            return Result.success(statistics);
        } catch (Exception e) {
            return Result.error("获取统计信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取相关笔记推荐
     */
    @GetMapping("/{id}/related")
    public Result getRelatedNotes(
            @PathVariable Long id,
            @RequestParam(defaultValue = "5") Integer limit) {
        try {
            List<Note> relatedNotes = noteService.getRelatedNotes(id, limit);
            return Result.success(relatedNotes);
        } catch (Exception e) {
            return Result.error("获取相关笔记失败: " + e.getMessage());
        }
    }

    /**
     * 获取热门笔记
     */
    @GetMapping("/popular")
    public Result getPopularNotes(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<Note> popularNotes = noteService.getPopularNotes(userId, limit);
            return Result.success(popularNotes);
        } catch (Exception e) {
            return Result.error("获取热门笔记失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近笔记
     */
    @GetMapping("/recent")
    public Result getRecentNotes(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<Note> recentNotes = noteService.getRecentNotes(userId, limit);
            return Result.success(recentNotes);
        } catch (Exception e) {
            return Result.error("获取最近笔记失败: " + e.getMessage());
        }
    }

    /**
     * 批量操作笔记
     */
    @PostMapping("/batch")
    public Result batchOperation(
            @RequestParam String operation,
            @RequestBody List<Long> noteIds) {
        try {
            switch (operation) {
                case "delete":
                    for (Long noteId : noteIds) {
                        noteService.deleteNote(noteId);
                    }
                    break;
                case "publish":
                    for (Long noteId : noteIds) {
                        noteService.publishNote(noteId);
                    }
                    break;
                case "archive":
                    for (Long noteId : noteIds) {
                        noteService.archiveNote(noteId);
                    }
                    break;
                default:
                    return Result.error("不支持的操作类型");
            }
            return Result.success("批量操作成功");
        } catch (Exception e) {
            return Result.error("批量操作失败: " + e.getMessage());
        }
    }

    /**
     * 增加笔记浏览量
     */
    @PostMapping("/{id}/view")
    public Result incrementView(@PathVariable Long id) {
        try {
            noteService.incrementViewCount(id);
            return Result.success("浏览量+1");
        } catch (Exception e) {
            return Result.error("增加浏览量失败: " + e.getMessage());
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