package com.example.controller;

import com.example.common.Result;
import com.example.entity.NoteTag;
import com.example.service.NoteTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 笔记标签关联控制器
 */
@RestController
@RequestMapping("/note-tags")
@Tag(name = "笔记标签关联管理", description = "笔记标签关联相关接口")
public class NoteTagController {

    @Autowired
    private NoteTagService noteTagService;

    /**
     * 添加笔记标签关联
     */
    @PostMapping("/add")
    @Operation(summary = "添加笔记标签关联", description = "为笔记添加单个标签关联")
    public Result addNoteTag(
            @Parameter(description = "笔记ID", required = true) @RequestParam Long noteId,
            @Parameter(description = "标签ID", required = true) @RequestParam Integer tagId) {
        try {
            boolean success = noteTagService.addNoteTag(noteId, tagId);
            return success ? Result.success(true) : Result.error("添加笔记标签关联失败");
        } catch (Exception e) {
            return Result.error("添加笔记标签关联异常: " + e.getMessage());
        }
    }

    /**
     * 批量添加笔记标签关联
     */
    @PostMapping("/batch-add")
    @Operation(summary = "批量添加笔记标签关联", description = "为笔记批量添加多个标签关联")
    public Result batchAddNoteTags(
            @Parameter(description = "笔记ID", required = true) @RequestParam Long noteId,
            @Parameter(description = "标签ID列表", required = true) @RequestBody List<Integer> tagIds) {
        try {
            boolean success = noteTagService.batchAddNoteTags(noteId, tagIds);
            return success ? Result.success(true) : Result.error("批量添加笔记标签关联失败");
        } catch (Exception e) {
            return Result.error("批量添加笔记标签关联异常: " + e.getMessage());
        }
    }

    /**
     * 删除笔记标签关联
     */
    @DeleteMapping("/remove")
    @Operation(summary = "删除笔记标签关联", description = "删除笔记与标签的关联")
    public Result removeNoteTag(
            @Parameter(description = "笔记ID", required = true) @RequestParam Long noteId,
            @Parameter(description = "标签ID", required = true) @RequestParam Integer tagId) {
        try {
            boolean success = noteTagService.removeNoteTag(noteId, tagId);
            return success ? Result.success(true) : Result.error("删除笔记标签关联失败");
        } catch (Exception e) {
            return Result.error("删除笔记标签关联异常: " + e.getMessage());
        }
    }

    /**
     * 删除笔记的所有标签关联
     */
    @DeleteMapping("/remove-all-by-note/{noteId}")
    @Operation(summary = "删除笔记的所有标签关联", description = "删除指定笔记的所有标签关联")
    public Result removeAllNoteTagsByNoteId(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId) {
        try {
            boolean success = noteTagService.removeAllNoteTagsByNoteId(noteId);
            return success ? Result.success(true) : Result.error("删除笔记所有标签关联失败");
        } catch (Exception e) {
            return Result.error("删除笔记所有标签关联异常: " + e.getMessage());
        }
    }

    /**
     * 删除标签的所有笔记关联
     */
    @DeleteMapping("/remove-all-by-tag/{tagId}")
    @Operation(summary = "删除标签的所有笔记关联", description = "删除指定标签的所有笔记关联")
    public Result removeAllNoteTagsByTagId(
            @Parameter(description = "标签ID", required = true) @PathVariable Integer tagId) {
        try {
            boolean success = noteTagService.removeAllNoteTagsByTagId(tagId);
            return success ? Result.success(true) : Result.error("删除标签所有笔记关联失败");
        } catch (Exception e) {
            return Result.error("删除标签所有笔记关联异常: " + e.getMessage());
        }
    }

    /**
     * 查询笔记的所有标签ID
     */
    @GetMapping("/tags-by-note/{noteId}")
    @Operation(summary = "查询笔记的所有标签ID", description = "获取指定笔记关联的所有标签ID")
    public Result getTagIdsByNoteId(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId) {
        try {
            List<Integer> tagIds = noteTagService.getTagsByNoteId(noteId);
            return Result.success(tagIds);
        } catch (Exception e) {
            return Result.error("查询笔记标签ID异常: " + e.getMessage());
        }
    }

    /**
     * 查询标签的所有笔记ID
     */
    @GetMapping("/notes-by-tag/{tagId}")
    @Operation(summary = "查询标签的所有笔记ID", description = "获取指定标签关联的所有笔记ID")
    public Result getNoteIdsByTagId(
            @Parameter(description = "标签ID", required = true) @PathVariable Integer tagId) {
        try {
            List<Long> noteIds = noteTagService.getNotesByTagId(tagId);
            return Result.success(noteIds);
        } catch (Exception e) {
            return Result.error("查询标签笔记ID异常: " + e.getMessage());
        }
    }

    /**
     * 查询笔记标签关联详情
     */
    @GetMapping("/details-by-note/{noteId}")
    @Operation(summary = "查询笔记标签关联详情", description = "获取指定笔记的所有标签关联详情")
    public Result getNoteTagsByNoteId(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId) {
        // 该接口已废弃，请使用 /tags-by-note/{noteId} 接口
        return Result.error("该接口已废弃，请使用 /tags-by-note/{noteId} 接口");
    }

    /**
     * 查询标签笔记关联详情
     */
    @GetMapping("/details-by-tag/{tagId}")
    @Operation(summary = "查询标签笔记关联详情", description = "获取指定标签的所有笔记关联详情")
    public Result getNoteTagsByTagId(
            @Parameter(description = "标签ID", required = true) @PathVariable Integer tagId) {
        // 该接口已废弃，请使用 /notes-by-tag/{tagId} 接口
        return Result.error("该接口已废弃，请使用 /notes-by-tag/{tagId} 接口");
    }

    /**
     * 检查笔记标签关联是否存在
     */
    @GetMapping("/exists")
    @Operation(summary = "检查笔记标签关联是否存在", description = "检查指定笔记和标签是否存在关联")
    public Result existsNoteTag(
            @Parameter(description = "笔记ID", required = true) @RequestParam Long noteId,
            @Parameter(description = "标签ID", required = true) @RequestParam Integer tagId) {
        try {
            boolean exists = noteTagService.existsNoteTag(noteId, tagId);
            return Result.success(exists);
        } catch (Exception e) {
            return Result.error("检查笔记标签关联异常: " + e.getMessage());
        }
    }

    /**
     * 统计笔记的标签数量
     */
    @GetMapping("/count-tags-by-note/{noteId}")
    @Operation(summary = "统计笔记的标签数量", description = "统计指定笔记关联的标签数量")
    public Result countTagsByNoteId(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId) {
        try {
            int count = noteTagService.countTagsByNoteId(noteId);
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("统计笔记标签数量异常: " + e.getMessage());
        }
    }

    /**
     * 统计标签的笔记数量
     */
    @GetMapping("/count-notes-by-tag/{tagId}")
    @Operation(summary = "统计标签的笔记数量", description = "统计指定标签关联的笔记数量")
    public Result countNotesByTagId(
            @Parameter(description = "标签ID", required = true) @PathVariable Integer tagId) {
        try {
            int count = noteTagService.countNotesByTagId(tagId);
            return Result.success(count);
        } catch (Exception e) {
            return Result.error("统计标签笔记数量异常: " + e.getMessage());
        }
    }

    /**
     * 更新笔记的标签关联
     */
    @PutMapping("/update-associations/{noteId}")
    @Operation(summary = "更新笔记的标签关联", description = "更新指定笔记的所有标签关联")
    public Result updateNoteTagAssociation(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId,
            @Parameter(description = "新的标签ID列表", required = true) @RequestBody List<Integer> tagIds) {
        try {
            boolean success = noteTagService.updateNoteTagAssociation(noteId, tagIds);
            return success ? Result.success(true) : Result.error("更新笔记标签关联失败");
        } catch (Exception e) {
            return Result.error("更新笔记标签关联异常: " + e.getMessage());
        }
    }

    /**
     * 获取相关笔记ID列表
     */
    @GetMapping("/related-notes/{noteId}")
    @Operation(summary = "获取相关笔记ID列表", description = "根据标签关联获取相关笔记ID列表")
    public Result getRelatedNoteIds(
            @Parameter(description = "笔记ID", required = true) @PathVariable Long noteId,
            @Parameter(description = "限制数量", required = false) @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<Long> relatedNoteIds = noteTagService.getRelatedNoteIds(noteId, limit);
            return Result.success(relatedNoteIds);
        } catch (Exception e) {
            return Result.error("获取相关笔记ID异常: " + e.getMessage());
        }
    }

    /**
     * 获取热门标签组合
     */
    @GetMapping("/popular-combinations/{userId}")
    @Operation(summary = "获取热门标签组合", description = "获取用户的热门标签组合")
    public Result getPopularTagCombinations(
            @Parameter(description = "用户ID", required = true) @PathVariable Integer userId,
            @Parameter(description = "限制数量", required = false) @RequestParam(defaultValue = "10") Integer limit) {
        try {
            List<Object> combinations = noteTagService.getPopularTagCombinations(userId, limit);
            return Result.success(combinations);
        } catch (Exception e) {
            return Result.error("获取热门标签组合异常: " + e.getMessage());
        }
    }


}