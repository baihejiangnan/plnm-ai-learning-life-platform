package com.example.service;

import com.example.entity.Note;
import com.example.entity.NoteCategory;
import com.example.entity.Tag;
import com.example.mapper.NoteMapper;
import com.example.mapper.NoteCategoryMapper;
import com.example.mapper.TagMapper;
import com.example.mapper.NoteTagMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 新增导入
import com.example.entity.NoteVersion;
import com.example.mapper.NoteVersionMapper;

/**
 * 笔记业务服务层
 */
@Service
public class NoteService {

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NoteCategoryMapper categoryMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private NoteTagMapper noteTagMapper;

    // 新增：笔记版本Mapper
    @Autowired
    private NoteVersionMapper noteVersionMapper;

    /**
     * 创建笔记
     */
    @Transactional
    public Note createNote(Note note) {
        // 设置创建时间
        note.setCreatedTime(LocalDateTime.now());
        note.setUpdatedTime(LocalDateTime.now());
        
        // 如果没有指定状态，默认为草稿
        if (note.getStatus() == null) {
            note.setStatus(0); // 0-草稿
        }
        // 兼容性：如果请求体意外携带了ID，则按更新处理（避免前端误用导致重复创建）
        if (note.getId() != null) {
            return updateNote(note);
        }
        // 计算字数
        if (note.getWordCount() == null) {
            note.setWordCount(estimateWordCount(note.getContent()));
        }
        // 初始化版本号
        if (note.getVersion() == null || note.getVersion() < 1) {
            note.setVersion(1);
        }
        
        // 插入笔记
        noteMapper.insert(note);
        
        // 创建版本记录（v1）
        NoteVersion v = new NoteVersion();
        v.setNoteId(note.getId());
        v.setVersionNumber(1);
        v.setTitle(note.getTitle());
        v.setContent(note.getContent());
        v.setChangeSummary(generateChangeSummary(note.getContent()));
        v.setWordCount(note.getWordCount());
        noteVersionMapper.insert(v);
        
        // 更新分类笔记数量
        if (note.getCategoryId() != null) {
            updateCategoryNoteCount(note.getCategoryId());
        }
        
        // 新增：处理标签关联（note_tag）并更新标签使用次数
        if (note.getTags() != null && !note.getTags().isEmpty()) {
            List<Integer> tagIds = note.getTags().stream()
                    .map(Tag::getId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            if (!tagIds.isEmpty()) {
                noteTagMapper.batchInsert(note.getId(), tagIds);
                // 更新每个标签的使用次数
                for (Integer tagId : tagIds) {
                    tagMapper.incrementUseCount(tagId);
                }
            }
        }
        
        return note;
    }

    /**
     * 更新笔记
     */
    @Transactional
    public Note updateNote(Note note) {
        Note existingNote = noteMapper.selectById(note.getId());
        if (existingNote == null) {
            throw new RuntimeException("笔记不存在");
        }
        
        // 计算字数
        if (note.getWordCount() == null) {
            note.setWordCount(estimateWordCount(note.getContent()));
        }
        
        // 计算下一个版本号（取note表version与历史表的最大版本的最大值+1，确保一致性）
        Integer maxVersion = noteVersionMapper.selectMaxVersionNumber(existingNote.getId());
        int baseVersion = existingNote.getVersion() != null ? existingNote.getVersion() : 0;
        int nextVersion = Math.max(baseVersion, maxVersion != null ? maxVersion : 0) + 1;
        
        // 插入版本记录（保存本次内容为新版本）
        NoteVersion v = new NoteVersion();
        v.setNoteId(existingNote.getId());
        v.setVersionNumber(nextVersion);
        v.setTitle(note.getTitle() != null ? note.getTitle() : existingNote.getTitle());
        v.setContent(note.getContent() != null ? note.getContent() : existingNote.getContent());
        v.setChangeSummary(generateChangeSummary(v.getContent()));
        int finalWordCount = note.getWordCount() != null ? note.getWordCount() : estimateWordCount(v.getContent());
        v.setWordCount(finalWordCount);
        note.setWordCount(finalWordCount);
        noteVersionMapper.insert(v);
        
        // 更新时间与版本号
        note.setUpdatedTime(LocalDateTime.now());
        note.setVersion(nextVersion);
        
        // 更新笔记基本信息
        noteMapper.updateById(note);
        
        // 新增：更新标签关联（如果此次更新包含 tags 字段）
        if (note.getTags() != null) {
            List<Integer> oldTagIds = noteTagMapper.selectTagIdsByNoteId(note.getId());
            List<Integer> newTagIds = note.getTags().stream()
                    .map(Tag::getId)
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());
            
            // 需要删除的标签
            List<Integer> toRemove = oldTagIds.stream()
                    .filter(id -> !newTagIds.contains(id))
                    .collect(Collectors.toList());
            // 需要新增的标签
            List<Integer> toAdd = newTagIds.stream()
                    .filter(id -> !oldTagIds.contains(id))
                    .collect(Collectors.toList());
            
            // 执行删除与新增
            if (!toRemove.isEmpty()) {
                noteTagMapper.batchDelete(note.getId(), toRemove);
                for (Integer tagId : toRemove) {
                    tagMapper.decrementUseCount(tagId);
                }
            }
            if (!toAdd.isEmpty()) {
                noteTagMapper.batchInsert(note.getId(), toAdd);
                for (Integer tagId : toAdd) {
                    tagMapper.incrementUseCount(tagId);
                }
            }
            // 若新列表为空且旧列表非空，则清空关联
            if (newTagIds.isEmpty() && !oldTagIds.isEmpty()) {
                noteTagMapper.deleteByNoteId(note.getId());
                for (Integer tagId : oldTagIds) {
                    tagMapper.decrementUseCount(tagId);
                }
            }
        }
        
        // 更新分类笔记数量
        if (existingNote.getCategoryId() != null) {
            updateCategoryNoteCount(existingNote.getCategoryId());
        }
        if (note.getCategoryId() != null && !note.getCategoryId().equals(existingNote.getCategoryId())) {
            updateCategoryNoteCount(note.getCategoryId());
        }
        
        return note;
    }

    /**
     * 删除笔记（软删除）
     */
    @Transactional
    public void deleteNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }
        
        // 软删除
        noteMapper.logicDeleteById(id);
        
        // 删除标签关联并更新使用次数
        List<Integer> tagIds = noteTagMapper.selectTagIdsByNoteId(id);
        noteTagMapper.deleteByNoteId(id);
        for (Integer tagId : tagIds) {
            tagMapper.decrementUseCount(tagId);
        }
        
        // 更新分类笔记数量
        if (note.getCategoryId() != null) {
            updateCategoryNoteCount(note.getCategoryId());
        }
    }

    /**
     * 彻底删除笔记
     */
    @Transactional
    public void permanentDeleteNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }
        
        // 删除标签关联
        List<Integer> tagIds = noteTagMapper.selectTagIdsByNoteId(id);
        noteTagMapper.deleteByNoteId(id);
        for (Integer tagId : tagIds) {
            tagMapper.decrementUseCount(tagId);
        }
        
        // 彻底删除笔记
        noteMapper.deleteById(id);
        
        // 更新分类笔记数量
        if (note.getCategoryId() != null) {
            updateCategoryNoteCount(note.getCategoryId());
        }
    }

    /**
     * 根据ID查询笔记
     */
    public Note getNoteById(Long id) {
        Note note = noteMapper.selectById(id);
        if (note != null) {
            // 加载关联的标签
            List<Tag> tags = tagMapper.selectByNoteId(id);
            note.setTags(tags);
            
            // 加载分类信息
            if (note.getCategoryId() != null) {
                NoteCategory category = categoryMapper.selectById(note.getCategoryId());
                note.setCategory(category);
            }
        }
        return note;
    }

    /**
     * 查询用户的笔记列表
     */
    public PageInfo<Note> getNotesByUserId(Integer userId, Integer status, Integer categoryId, 
                                       String keyword, Integer page, Integer size, Integer tagId) {
        // 使用PageHelper进行分页
        PageHelper.startPage(page, size);
        
        List<Note> notes;
        // 优先：按标签过滤
        if (tagId != null && status != null) {
            notes = noteMapper.selectByUserIdAndStatusAndTagId(userId, status, tagId);
        } else if (tagId != null) {
            notes = noteMapper.selectByUserIdAndTagId(userId, tagId);
        } else if (status != null) {
            notes = noteMapper.selectByUserIdAndStatus(userId, status);
        } else {
            notes = noteMapper.selectByUserId(userId);
        }
        
        // 关键字二次过滤（如需）
        if (keyword != null && !keyword.trim().isEmpty()) {
            notes = notes.stream()
                    .filter(n -> (n.getTitle() != null && n.getTitle().contains(keyword)) ||
                                 (n.getContent() != null && n.getContent().contains(keyword)))
                    .toList();
        }
        
        // 加载关联信息
        for (Note note : notes) {
            List<Tag> tags = tagMapper.selectByNoteId(note.getId());
            note.setTags(tags);
            
            if (note.getCategoryId() != null) {
                NoteCategory category = categoryMapper.selectById(note.getCategoryId());
                note.setCategory(category);
            }
        }
        
        return new PageInfo<>(notes);
    }

    /**
     * 搜索笔记
     */
    public PageInfo<Note> searchNotes(Integer userId, String keyword, Integer page, Integer size) {
        // 使用PageHelper进行分页
        PageHelper.startPage(page, size);
        
        List<Note> notes = noteMapper.searchByContent(userId, keyword);
        
        return new PageInfo<>(notes);
    }

    /**
     * 获取笔记统计信息
     */
    public Map<String, Object> getNoteStatistics(Integer userId) {
        // 简化统计实现
        int totalCount = noteMapper.countByUserId(userId);
        int publishedCount = noteMapper.countByUserIdAndStatus(userId, 1);
        return Map.of(
            "totalCount", totalCount,
            "publishedCount", publishedCount,
            "draftCount", noteMapper.countByUserIdAndStatus(userId, 0)
        );
    }

    /**
     * 更新分类的笔记数量
     */
    private void updateCategoryNoteCount(Integer categoryId) {
        int count = categoryMapper.countNotesByCategoryId(categoryId);
        categoryMapper.updateNoteCount(categoryId, count);
    }

    /**
     * 发布笔记
     */
    public void publishNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note != null) {
            note.setStatus(1); // 1-已发布
            noteMapper.updateById(note);
        }
    }

    /**
     * 归档笔记
     */
    public void archiveNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note != null) {
            note.setStatus(2); // 2-已归档
            noteMapper.updateById(note);
        }
    }

    /**
     * 恢复笔记
     */
    public void restoreNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note != null) {
            note.setStatus(1); // 恢复为已发布状态
            noteMapper.updateById(note);
        }
    }

    /**
     * 获取相关笔记推荐
     */
    public List<Note> getRelatedNotes(Long noteId, Integer limit) {
        List<Long> relatedNoteIds = noteTagMapper.selectRelatedNoteIds(noteId, limit);
        // 由于selectByIds方法不存在，暂时返回空列表
        return List.of();
    }

    /**
     * 增加笔记浏览次数
     */
    public void incrementViewCount(Long id) {
        noteMapper.incrementViewCount(id);
    }

    /**
     * 获取热门笔记
     */
    public List<Note> getPopularNotes(Integer userId, Integer limit) {
        // 由于selectPopularByUserId方法不存在，使用现有方法替代
        return noteMapper.selectRecentByUserId(userId, limit);
    }

    /**
     * 获取最近笔记
     */
    public List<Note> getRecentNotes(Integer userId, Integer limit) {
        return noteMapper.selectRecentByUserId(userId, limit);
    }

    /**
     * 粗略估算字数（移除HTML标签后按长度计算）
     */
    private int estimateWordCount(String content) {
        if (content == null || content.isEmpty()) return 0;
        String plain = content.replaceAll("<[^>]*>", "").trim();
        return plain.length();
    }

    /**
     * 生成变更摘要（取前200字符）
     */
    private String generateChangeSummary(String content) {
        if (content == null) return null;
        String plain = content.replaceAll("<[^>]*>", "").trim();
        return plain.length() > 200 ? plain.substring(0, 200) + "..." : plain;
    }
}