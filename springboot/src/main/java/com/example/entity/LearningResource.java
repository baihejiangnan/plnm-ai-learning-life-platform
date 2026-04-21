package com.example.entity;

import java.time.LocalDateTime;

/**
 * 学习模块-资源实体
 */
public class LearningResource {
    private Integer id;
    private Integer userId;
    private String title;
    private String type; // article / video / book
    private String source;
    private String link;
    private Integer categoryId; // 可为空，关联课程分类
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public LocalDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(LocalDateTime createdTime) { this.createdTime = createdTime; }

    public LocalDateTime getUpdatedTime() { return updatedTime; }
    public void setUpdatedTime(LocalDateTime updatedTime) { this.updatedTime = updatedTime; }
}