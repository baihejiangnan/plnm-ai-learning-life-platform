package com.example.entity;

import java.time.LocalDateTime;

/**
 * 笔记标签关联实体类
 */
public class NoteTag {
    
    /**
     * 笔记ID
     */
    private Long noteId;
    
    /**
     * 标签ID
     */
    private Integer tagId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    // 关联对象
    private Note note;
    private Tag tag;
    
    // 构造方法
    public NoteTag() {
        this.createdAt = LocalDateTime.now();
    }
    
    public NoteTag(Long noteId, Integer tagId) {
        this.noteId = noteId;
        this.tagId = tagId;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getter和Setter方法
    public Long getNoteId() {
        return noteId;
    }
    
    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }
    
    public Integer getTagId() {
        return tagId;
    }
    
    public void setTagId(Integer tagId) {
        this.tagId = tagId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Note getNote() {
        return note;
    }
    
    public void setNote(Note note) {
        this.note = note;
    }
    
    public Tag getTag() {
        return tag;
    }
    
    public void setTag(Tag tag) {
        this.tag = tag;
    }
    
    @Override
    public String toString() {
        return "NoteTag{" +
                "noteId=" + noteId +
                ", tagId=" + tagId +
                ", createdAt=" + createdAt +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        NoteTag noteTag = (NoteTag) o;
        
        if (!noteId.equals(noteTag.noteId)) return false;
        return tagId.equals(noteTag.tagId);
    }
    
    @Override
    public int hashCode() {
        int result = noteId.hashCode();
        result = 31 * result + tagId.hashCode();
        return result;
    }
}