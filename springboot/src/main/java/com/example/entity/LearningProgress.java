package com.example.entity;

import java.time.LocalDateTime;

/**
 * 学习模块-进度实体
 */
public class LearningProgress {
    private Integer id;
    private Integer userId;
    private String course; // 课程名称
    private Integer percent; // 0~100
    private String note; // 备注
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getCourse() { return course; }
    public void setCourse(String course) { this.course = course; }

    public Integer getPercent() { return percent; }
    public void setPercent(Integer percent) { this.percent = percent; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}