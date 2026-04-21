package com.example.service;

import com.example.entity.Tag;
import com.example.mapper.TagMapper;
import com.example.mapper.NoteTagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * 标签业务服务层
 */
@Service
public class TagService {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private NoteTagMapper noteTagMapper;

    /**
     * 创建标签
     */
    @Transactional
    public Tag createTag(Tag tag) {
        // 检查同名标签
        Tag existing = tagMapper.selectByUserIdAndName(tag.getUserId(), tag.getName());
        if (existing != null) {
            throw new RuntimeException("标签名称已存在");
        }
        
        // 设置创建时间
        tag.setCreatedTime(LocalDateTime.now());
        tag.setUpdatedTime(LocalDateTime.now());
        
        // 初始化使用次数
        tag.setUseCount(0);
        
        tagMapper.insert(tag);
        return tag;
    }

    /**
     * 批量创建标签
     */
    @Transactional
    public List<Tag> createTags(List<Tag> tags) {
        List<Tag> createdTags = new ArrayList<>();
        
        for (Tag tag : tags) {
            // 检查是否已存在
            Tag existing = tagMapper.selectByUserIdAndName(tag.getUserId(), tag.getName());
            if (existing == null) {
                tag.setCreatedTime(LocalDateTime.now());
                tag.setUpdatedTime(LocalDateTime.now());
                tag.setUseCount(0);
                createdTags.add(tag);
            } else {
                createdTags.add(existing);
            }
        }
        
        // 批量插入新标签
        List<Tag> newTags = createdTags.stream()
            .filter(tag -> tag.getId() == null)
            .collect(java.util.stream.Collectors.toList());
            
        if (!newTags.isEmpty()) {
            tagMapper.batchInsert(newTags);
        }
        
        return createdTags;
    }

    /**
     * 更新标签
     */
    @Transactional
    public Tag updateTag(Tag tag) {
        Tag existing = tagMapper.selectById(tag.getId());
        if (existing == null) {
            throw new RuntimeException("标签不存在");
        }
        
        // 检查同名标签（排除自己）
        Tag sameName = tagMapper.selectByUserIdAndName(tag.getUserId(), tag.getName());
        if (sameName != null && !sameName.getId().equals(tag.getId())) {
            throw new RuntimeException("标签名称已存在");
        }
        
        tag.setUpdatedTime(LocalDateTime.now());
        tagMapper.updateById(tag);
        return tag;
    }

    /**
     * 删除标签
     */
    @Transactional
    public void deleteTag(Integer id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        
        // 删除标签与笔记的关联
        noteTagMapper.deleteByTagId(id);
        
        // 删除标签
        tagMapper.deleteById(id);
    }

    /**
     * 批量删除未使用的标签
     */
    @Transactional
    public int deleteUnusedTags(Integer userId) {
        List<Tag> unusedTags = tagMapper.selectUnusedByUserId(userId);
        int deletedCount = 0;
        
        for (Tag tag : unusedTags) {
            tagMapper.deleteById(tag.getId());
            deletedCount++;
        }
        
        return deletedCount;
    }

    /**
     * 根据ID查询标签
     */
    public Tag getTagById(Integer id) {
        return tagMapper.selectById(id);
    }

    /**
     * 查询用户的所有标签
     */
    public List<Tag> getTagsByUserId(Integer userId) {
        return tagMapper.selectByUserId(userId);
    }

    /**
     * 根据笔记ID查询关联的标签
     */
    public List<Tag> getTagsByNoteId(Long noteId) {
        return tagMapper.selectByNoteId(noteId);
    }

    /**
     * 搜索标签
     */
    public List<Tag> searchTags(Integer userId, String keyword) {
        return tagMapper.searchByName(userId, keyword);
    }

    /**
     * 获取热门标签
     */
    public List<Tag> getPopularTags(Integer userId, Integer limit) {
        return tagMapper.selectPopularByUserId(userId, limit);
    }

    /**
     * 获取未使用的标签
     */
    public List<Tag> getUnusedTags(Integer userId) {
        return tagMapper.selectUnusedByUserId(userId);
    }

    /**
     * 智能推荐标签（基于笔记内容）
     */
    public List<Tag> recommendTags(Integer userId, String content, Integer limit) {
        // 简单的关键词匹配推荐
        List<Tag> allTags = tagMapper.selectByUserId(userId);
        List<Tag> recommendedTags = new ArrayList<>();
        
        String lowerContent = content.toLowerCase();
        
        for (Tag tag : allTags) {
            String tagName = tag.getName().toLowerCase();
            if (lowerContent.contains(tagName) || tagName.contains(lowerContent)) {
                recommendedTags.add(tag);
                if (recommendedTags.size() >= limit) {
                    break;
                }
            }
        }
        
        // 如果推荐的标签不够，补充热门标签
        if (recommendedTags.size() < limit) {
            List<Tag> popularTags = getPopularTags(userId, limit - recommendedTags.size());
            for (Tag popularTag : popularTags) {
                if (!recommendedTags.contains(popularTag)) {
                    recommendedTags.add(popularTag);
                }
            }
        }
        
        return recommendedTags;
    }

    /**
     * 获取或创建标签（如果不存在则创建）
     */
    @Transactional
    public Tag getOrCreateTag(Integer userId, String tagName, String color) {
        Tag existing = tagMapper.selectByUserIdAndName(userId, tagName);
        if (existing != null) {
            return existing;
        }
        
        // 创建新标签
        Tag newTag = new Tag();
        newTag.setUserId(userId);
        newTag.setName(tagName);
        newTag.setColor(color != null ? color : "#1890ff"); // 默认蓝色
        newTag.setCreatedTime(LocalDateTime.now());
        newTag.setUpdatedTime(LocalDateTime.now());
        newTag.setUseCount(0);
        
        tagMapper.insert(newTag);
        return newTag;
    }

    /**
     * 批量获取或创建标签
     */
    @Transactional
    public List<Tag> getOrCreateTags(Integer userId, List<String> tagNames) {
        List<Tag> tags = new ArrayList<>();
        
        for (String tagName : tagNames) {
            Tag tag = getOrCreateTag(userId, tagName, null);
            tags.add(tag);
        }
        
        return tags;
    }

    /**
     * 合并标签（将源标签的所有关联转移到目标标签）
     */
    @Transactional
    public void mergeTags(Integer sourceTagId, Integer targetTagId) {
        Tag sourceTag = tagMapper.selectById(sourceTagId);
        Tag targetTag = tagMapper.selectById(targetTagId);
        
        if (sourceTag == null || targetTag == null) {
            throw new RuntimeException("标签不存在");
        }
        
        if (sourceTagId.equals(targetTagId)) {
            throw new RuntimeException("不能合并相同的标签");
        }
        
        // 获取源标签关联的所有笔记
        List<Long> noteIds = noteTagMapper.selectNoteIdsByTagId(sourceTagId);
        
        // 将这些笔记关联到目标标签
        for (Long noteId : noteIds) {
            // 检查是否已经关联
            if (noteTagMapper.exists(noteId, targetTagId) == 0) {
                noteTagMapper.insert(noteId, targetTagId);
                tagMapper.incrementUseCount(targetTagId);
            }
        }
        
        // 删除源标签的所有关联
        noteTagMapper.deleteByTagId(sourceTagId);
        
        // 删除源标签
        tagMapper.deleteById(sourceTagId);
    }

    /**
     * 重命名标签
     */
    @Transactional
    public Tag renameTag(Integer tagId, String newName) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        
        // 检查新名称是否已存在
        Tag existing = tagMapper.selectByUserIdAndName(tag.getUserId(), newName);
        if (existing != null && !existing.getId().equals(tagId)) {
            throw new RuntimeException("标签名称已存在");
        }
        
        tag.setName(newName);
        tag.setUpdatedTime(LocalDateTime.now());
        tagMapper.updateById(tag);
        
        return tag;
    }

    /**
     * 统计用户标签数量
     */
    public int countUserTags(Integer userId) {
        return tagMapper.countByUserId(userId);
    }

    /**
     * 获取标签云数据（用于可视化）
     */
    public List<java.util.Map<String, Object>> getTagCloud(Integer userId) {
        List<Tag> tags = tagMapper.selectPopularByUserId(userId, 50); // 最多50个标签
        List<java.util.Map<String, Object>> tagCloud = new ArrayList<>();
        
        for (Tag tag : tags) {
            java.util.Map<String, Object> item = new java.util.HashMap<>();
            item.put("name", tag.getName());
            item.put("value", tag.getUseCount());
            item.put("color", tag.getColor());
            tagCloud.add(item);
        }
        
        return tagCloud;
    }
}