package com.example.service;

import com.example.entity.NoteCategory;
import com.example.mapper.NoteCategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记分类业务服务层
 */
@Service
public class NoteCategoryService {

    @Autowired
    private NoteCategoryMapper categoryMapper;

    /**
     * 创建分类
     */
    @Transactional
    public NoteCategory createCategory(NoteCategory category) {
        // 检查同名分类
        NoteCategory existing = categoryMapper.selectByUserIdAndName(category.getUserId(), category.getName());
        if (existing != null) {
            throw new RuntimeException("分类名称已存在");
        }
        
        // 设置创建时间
        category.setCreatedTime(LocalDateTime.now());
        category.setUpdatedTime(LocalDateTime.now());
        
        // 设置排序号
        if (category.getSortOrder() == null) {
            int maxOrder = categoryMapper.getMaxSortOrderByUserId(category.getUserId());
            category.setSortOrder(maxOrder + 1);
        }
        
        // 初始化笔记数量
        category.setNoteCount(0);
        
        categoryMapper.insert(category);
        return category;
    }

    /**
     * 更新分类
     */
    @Transactional
    public NoteCategory updateCategory(NoteCategory category) {
        NoteCategory existing = categoryMapper.selectById(category.getId());
        if (existing == null) {
            throw new RuntimeException("分类不存在");
        }
        
        // 检查同名分类（排除自己）
        NoteCategory sameName = categoryMapper.selectByUserIdAndName(category.getUserId(), category.getName());
        if (sameName != null && !sameName.getId().equals(category.getId())) {
            throw new RuntimeException("分类名称已存在");
        }
        
        category.setUpdatedTime(LocalDateTime.now());
        categoryMapper.updateById(category);
        return category;
    }

    /**
     * 删除分类
     */
    @Transactional
    public void deleteCategory(Integer id) {
        NoteCategory category = categoryMapper.selectById(id);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        // 检查是否有子分类
        int childrenCount = categoryMapper.countChildrenByCategoryId(id);
        if (childrenCount > 0) {
            throw new RuntimeException("该分类下还有子分类，无法删除");
        }
        
        // 检查是否有笔记
        int noteCount = categoryMapper.countNotesByCategoryId(id);
        if (noteCount > 0) {
            throw new RuntimeException("该分类下还有笔记，无法删除");
        }
        
        categoryMapper.deleteById(id);
    }

    /**
     * 根据ID查询分类
     */
    public NoteCategory getCategoryById(Integer id) {
        return categoryMapper.selectById(id);
    }

    /**
     * 查询用户的所有分类
     */
    public List<NoteCategory> getCategoriesByUserId(Integer userId) {
        return categoryMapper.selectByUserId(userId);
    }

    /**
     * 查询根分类（树形结构的根节点）
     */
    public List<NoteCategory> getRootCategories(Integer userId) {
        return categoryMapper.selectRootByUserId(userId);
    }

    /**
     * 查询子分类
     */
    public List<NoteCategory> getChildCategories(Integer parentId) {
        return categoryMapper.selectByParentId(parentId);
    }

    /**
     * 构建分类树
     */
    public List<NoteCategory> buildCategoryTree(Integer userId) {
        List<NoteCategory> rootCategories = getRootCategories(userId);
        for (NoteCategory root : rootCategories) {
            buildChildren(root);
        }
        return rootCategories;
    }

    /**
     * 递归构建子分类
     */
    private void buildChildren(NoteCategory parent) {
        List<NoteCategory> children = getChildCategories(parent.getId());
        parent.setChildren(children);
        for (NoteCategory child : children) {
            buildChildren(child);
        }
    }

    /**
     * 更新分类排序
     */
    @Transactional
    public void updateCategorySort(List<NoteCategory> categories) {
        for (int i = 0; i < categories.size(); i++) {
            NoteCategory category = categories.get(i);
            categoryMapper.updateSortOrder(category.getId(), i + 1);
        }
    }

    /**
     * 移动分类到新的父分类下
     */
    @Transactional
    public void moveCategory(Integer categoryId, Integer newParentId) {
        NoteCategory category = categoryMapper.selectById(categoryId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        
        // 检查是否会形成循环引用
        if (newParentId != null && isDescendant(categoryId, newParentId)) {
            throw new RuntimeException("不能将分类移动到其子分类下");
        }
        
        category.setParentId(newParentId);
        category.setUpdatedTime(LocalDateTime.now());
        categoryMapper.updateById(category);
    }

    /**
     * 检查是否为子孙分类（防止循环引用）
     */
    private boolean isDescendant(Integer ancestorId, Integer descendantId) {
        if (ancestorId.equals(descendantId)) {
            return true;
        }
        
        List<NoteCategory> children = getChildCategories(ancestorId);
        for (NoteCategory child : children) {
            if (isDescendant(child.getId(), descendantId)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 获取分类路径（面包屑导航）
     */
    public List<NoteCategory> getCategoryPath(Integer categoryId) {
        List<NoteCategory> path = new java.util.ArrayList<>();
        NoteCategory current = categoryMapper.selectById(categoryId);
        
        while (current != null) {
            path.add(0, current); // 插入到开头
            if (current.getParentId() != null) {
                current = categoryMapper.selectById(current.getParentId());
            } else {
                break;
            }
        }
        
        return path;
    }

    /**
     * 刷新分类笔记数量
     */
    @Transactional
    public void refreshCategoryNoteCounts(Integer userId) {
        List<NoteCategory> categories = getCategoriesByUserId(userId);
        for (NoteCategory category : categories) {
            int noteCount = categoryMapper.countNotesByCategoryId(category.getId());
            categoryMapper.updateNoteCount(category.getId(), noteCount);
        }
    }

    /**
     * 复制分类结构
     */
    @Transactional
    public void copyCategoryStructure(Integer fromUserId, Integer toUserId) {
        List<NoteCategory> sourceCategories = getCategoriesByUserId(fromUserId);
        
        // 创建ID映射表
        java.util.Map<Integer, Integer> idMapping = new java.util.HashMap<>();
        
        // 先创建所有分类（不设置父分类）
        for (NoteCategory source : sourceCategories) {
            NoteCategory copy = new NoteCategory();
            copy.setUserId(toUserId);
            copy.setName(source.getName());
            copy.setDescription(source.getDescription());
            copy.setColor(source.getColor());
            copy.setIcon(source.getIcon());
            copy.setSortOrder(source.getSortOrder());
            copy.setNoteCount(0);
            copy.setCreatedTime(LocalDateTime.now());
            copy.setUpdatedTime(LocalDateTime.now());
            
            categoryMapper.insert(copy);
            idMapping.put(source.getId(), copy.getId());
        }
        
        // 再设置父分类关系
        for (NoteCategory source : sourceCategories) {
            if (source.getParentId() != null) {
                Integer newId = idMapping.get(source.getId());
                Integer newParentId = idMapping.get(source.getParentId());
                
                NoteCategory copy = categoryMapper.selectById(newId);
                copy.setParentId(newParentId);
                categoryMapper.updateById(copy);
            }
        }
    }
}