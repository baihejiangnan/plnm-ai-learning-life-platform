package com.example.service;

import com.example.entity.NoteTag;
import com.example.mapper.NoteTagMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 笔记标签关联业务服务层
 */
@Service
@Transactional
public class NoteTagService {

    @Autowired
    private NoteTagMapper noteTagMapper;

    /**
     * 添加笔记标签关联
     */
    public boolean addNoteTag(Long noteId, Integer tagId) {
        if (noteId == null || tagId == null) {
            return false;
        }
        
        // 检查关联是否已存在
        if (existsNoteTag(noteId, tagId)) {
            return true;
        }
        
        return noteTagMapper.insert(noteId, tagId) > 0;
    }

    /**
     * 批量添加笔记标签关联
     */
    public boolean batchAddNoteTags(Long noteId, List<Integer> tagIds) {
        if (noteId == null || tagIds == null || tagIds.isEmpty()) {
            return false;
        }
        
        return noteTagMapper.batchInsert(noteId, tagIds) > 0;
    }

    /**
     * 删除笔记标签关联
     */
    public boolean removeNoteTag(Long noteId, Integer tagId) {
        if (noteId == null || tagId == null) {
            return false;
        }
        return noteTagMapper.delete(noteId, tagId) > 0;
    }

    /**
     * 删除笔记的所有标签关联
     */
    public boolean removeAllNoteTagsByNoteId(Long noteId) {
        if (noteId == null) {
            return false;
        }
        return noteTagMapper.deleteByNoteId(noteId) >= 0;
    }

    /**
     * 删除标签的所有笔记关联
     */
    public boolean removeAllNoteTagsByTagId(Integer tagId) {
        if (tagId == null) {
            return false;
        }
        return noteTagMapper.deleteByTagId(tagId) >= 0;
    }

    /**
     * 批量删除笔记标签关联
     */
    public boolean batchRemoveNoteTags(Long noteId, List<Integer> tagIds) {
        if (noteId == null || tagIds == null || tagIds.isEmpty()) {
            return false;
        }
        return noteTagMapper.batchDelete(noteId, tagIds) > 0;
    }

    /**
     * 查询笔记的所有标签ID
     */
    public List<Integer> getTagIdsByNoteId(Long noteId) {
        if (noteId == null) {
            return List.of();
        }
        return noteTagMapper.selectTagIdsByNoteId(noteId);
    }

    /**
     * 查询标签的所有笔记ID
     */
    public List<Long> getNoteIdsByTagId(Integer tagId) {
        if (tagId == null) {
            return List.of();
        }
        return noteTagMapper.selectNoteIdsByTagId(tagId);
    }

    /**
     * 查询笔记的标签ID列表
     */
    public List<Integer> getTagsByNoteId(Long noteId) {
        if (noteId == null) {
            return List.of();
        }
        return noteTagMapper.selectTagIdsByNoteId(noteId);
    }

    /**
     * 查询标签的笔记ID列表
     */
    public List<Long> getNotesByTagId(Integer tagId) {
        if (tagId == null) {
            return List.of();
        }
        return noteTagMapper.selectNoteIdsByTagId(tagId);
    }

    /**
     * 检查笔记标签关联是否存在
     */
    public boolean existsNoteTag(Long noteId, Integer tagId) {
        if (noteId == null || tagId == null) {
            return false;
        }
        return noteTagMapper.exists(noteId, tagId) > 0;
    }

    /**
     * 统计笔记的标签数量
     */
    public int countTagsByNoteId(Long noteId) {
        if (noteId == null) {
            return 0;
        }
        return noteTagMapper.countTagsByNoteId(noteId);
    }

    /**
     * 统计标签的笔记数量
     */
    public int countNotesByTagId(Integer tagId) {
        if (tagId == null) {
            return 0;
        }
        return noteTagMapper.countNotesByTagId(tagId);
    }

    /**
     * 更新笔记的标签关联
     */
    public boolean updateNoteTagAssociation(Long noteId, List<Integer> newTagIds) {
        if (noteId == null) {
            return false;
        }
        
        // 获取当前标签ID列表
        List<Integer> oldTagIds = getTagsByNoteId(noteId);
        
        // 如果新标签列表为空，删除所有关联
        if (newTagIds == null || newTagIds.isEmpty()) {
            return removeAllNoteTagsByNoteId(noteId);
        }
        
        // 找出需要删除的标签
        List<Integer> toRemove = oldTagIds.stream()
                .filter(tagId -> !newTagIds.contains(tagId))
                .collect(Collectors.toList());
        
        // 找出需要添加的标签
        List<Integer> toAdd = newTagIds.stream()
                .filter(tagId -> !oldTagIds.contains(tagId))
                .collect(Collectors.toList());
        
        boolean success = true;
        
        // 删除不需要的关联
        if (!toRemove.isEmpty()) {
            success &= noteTagMapper.batchDelete(noteId, toRemove) >= 0;
        }
        
        // 添加新的关联
        if (!toAdd.isEmpty()) {
            success &= noteTagMapper.batchInsert(noteId, toAdd) > 0;
        }
        
        return success;
    }

    /**
     * 获取相关笔记ID列表
     */
    public List<Long> getRelatedNoteIds(Long noteId, Integer limit) {
        if (noteId == null) {
            return List.of();
        }
        return noteTagMapper.selectRelatedNoteIds(noteId, limit != null ? limit : 10);
    }

    /**
     * 获取热门标签组合
     */
    public List<Object> getPopularTagCombinations(Integer userId, Integer limit) {
        if (userId == null) {
            return List.of();
        }
        return noteTagMapper.selectPopularTagCombinations(userId, limit != null ? limit : 10);
    }


}