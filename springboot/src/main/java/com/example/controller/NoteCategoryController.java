package com.example.controller;

import com.example.entity.NoteCategory;
import com.example.service.NoteCategoryService;
import com.example.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 笔记分类控制器
 */
@RestController
@RequestMapping("/api/categories")
@CrossOrigin
public class NoteCategoryController {

    @Autowired
    private NoteCategoryService categoryService;

    /**
     * 创建分类
     */
    @PostMapping
    public Result createCategory(@RequestBody NoteCategory category) {
        try {
            NoteCategory createdCategory = categoryService.createCategory(category);
            return Result.success(createdCategory);
        } catch (Exception e) {
            return Result.error("创建分类失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类
     */
    @PutMapping("/{id}")
    public Result updateCategory(@PathVariable Integer id, @RequestBody NoteCategory category) {
        try {
            category.setId(id);
            NoteCategory updatedCategory = categoryService.updateCategory(category);
            return Result.success(updatedCategory);
        } catch (Exception e) {
            return Result.error("更新分类失败: " + e.getMessage());
        }
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/{id}")
    public Result deleteCategory(@PathVariable Integer id) {
        try {
            categoryService.deleteCategory(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除分类失败: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询分类
     */
    @GetMapping("/{id}")
    public Result getCategoryById(@PathVariable Integer id) {
        try {
            NoteCategory category = categoryService.getCategoryById(id);
            if (category != null) {
                return Result.success(category);
            } else {
                return Result.error("分类不存在");
            }
        } catch (Exception e) {
            return Result.error("查询分类失败: " + e.getMessage());
        }
    }

    /**
     * 查询用户的所有分类
     */
    @GetMapping
    public Result getCategoriesByUserId(@RequestParam Integer userId) {
        try {
            List<NoteCategory> categories = categoryService.getCategoriesByUserId(userId);
            return Result.success(categories);
        } catch (Exception e) {
            return Result.error("查询分类列表失败: " + e.getMessage());
        }
    }

    /**
     * 查询分类树结构
     */
    @GetMapping("/tree")
    public Result getCategoryTree(@RequestParam Integer userId) {
        try {
            List<NoteCategory> categoryTree = categoryService.buildCategoryTree(userId);
            return Result.success(categoryTree);
        } catch (Exception e) {
            return Result.error("查询分类树失败: " + e.getMessage());
        }
    }

    /**
     * 查询根分类
     */
    @GetMapping("/root")
    public Result getRootCategories(@RequestParam Integer userId) {
        try {
            List<NoteCategory> rootCategories = categoryService.getRootCategories(userId);
            return Result.success(rootCategories);
        } catch (Exception e) {
            return Result.error("查询根分类失败: " + e.getMessage());
        }
    }

    /**
     * 查询子分类
     */
    @GetMapping("/{parentId}/children")
    public Result getChildCategories(@PathVariable Integer parentId) {
        try {
            List<NoteCategory> childCategories = categoryService.getChildCategories(parentId);
            return Result.success(childCategories);
        } catch (Exception e) {
            return Result.error("查询子分类失败: " + e.getMessage());
        }
    }

    /**
     * 移动分类
     */
    @PutMapping("/{id}/move")
    public Result moveCategory(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer newParentId) {
        try {
            categoryService.moveCategory(id, newParentId);
            return Result.success("移动成功");
        } catch (Exception e) {
            return Result.error("移动分类失败: " + e.getMessage());
        }
    }

    /**
     * 更新分类排序
     */
    @PutMapping("/sort")
    public Result updateCategorySort(@RequestBody List<NoteCategory> categories) {
        try {
            categoryService.updateCategorySort(categories);
            return Result.success("排序更新成功");
        } catch (Exception e) {
            return Result.error("更新排序失败: " + e.getMessage());
        }
    }

    /**
     * 获取分类路径（面包屑导航）
     */
    @GetMapping("/{id}/path")
    public Result getCategoryPath(@PathVariable Integer id) {
        try {
            List<NoteCategory> path = categoryService.getCategoryPath(id);
            return Result.success(path);
        } catch (Exception e) {
            return Result.error("获取分类路径失败: " + e.getMessage());
        }
    }

    /**
     * 刷新分类笔记数量
     */
    @PostMapping("/refresh-counts")
    public Result refreshCategoryNoteCounts(@RequestParam Integer userId) {
        try {
            categoryService.refreshCategoryNoteCounts(userId);
            return Result.success("刷新成功");
        } catch (Exception e) {
            return Result.error("刷新分类笔记数量失败: " + e.getMessage());
        }
    }

    /**
     * 复制分类结构
     */
    @PostMapping("/copy")
    public Result copyCategoryStructure(
            @RequestParam Integer fromUserId,
            @RequestParam Integer toUserId) {
        try {
            categoryService.copyCategoryStructure(fromUserId, toUserId);
            return Result.success("复制成功");
        } catch (Exception e) {
            return Result.error("复制分类结构失败: " + e.getMessage());
        }
    }

    /**
     * 批量创建默认分类
     */
    @PostMapping("/init-default")
    public Result initDefaultCategories(@RequestParam Integer userId) {
        try {
            // 创建一些默认分类
            String[] defaultCategories = {"学习笔记", "工作记录", "生活随笔", "技术文档", "读书笔记"};
            String[] colors = {"#1890ff", "#52c41a", "#faad14", "#f5222d", "#722ed1"};
            String[] icons = {"book", "work", "life", "code", "read"};
            
            for (int i = 0; i < defaultCategories.length; i++) {
                NoteCategory category = new NoteCategory();
                category.setUserId(userId);
                category.setName(defaultCategories[i]);
                category.setColor(colors[i]);
                category.setIcon(icons[i]);
                category.setDescription("默认" + defaultCategories[i] + "分类");
                
                try {
                    categoryService.createCategory(category);
                } catch (Exception e) {
                    // 忽略重复创建的错误
                }
            }
            
            return Result.success("初始化默认分类成功");
        } catch (Exception e) {
            return Result.error("初始化默认分类失败: " + e.getMessage());
        }
    }
}