package com.example.entity;

import java.time.LocalDateTime;

/**
 * 笔记版本实体
 */
public class NoteVersion {
    /** 主键ID */
    private Long id;
    /** 对应的笔记ID */
    private Long noteId;
    /** 版本号，从1开始递增 */
    private Integer versionNumber;
    /** 版本标题快照 */
    private String title;
    /** 版本内容快照 */
    private String content;
    /** 变更摘要（可选） */
    private String changeSummary;
    /** 字数统计 */
    private Integer wordCount;
    /** 版本创建时间 */
    private LocalDateTime createdTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }
}