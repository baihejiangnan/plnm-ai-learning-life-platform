package com.example.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记分类实体类
 */
public class NoteCategory {

    /** 分类ID */
    private Integer id;
    /** 用户ID */
    private Integer userId;
    /** 分类名称 */
    private String name;
    /** 分类描述 */
    private String description;
    /** 分类颜色 */
    private String color;
    /** 分类图标 */
    private String icon;
    /** 父分类ID */
    private Integer parentId;
    /** 排序序号 */
    private Integer sortOrder;
    /** 笔记数量 */
    private Integer noteCount;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 更新时间 */
    private LocalDateTime updatedTime;

    // 关联对象
    /** 父分类 */
    private NoteCategory parent;
    /** 子分类列表 */
    private List<NoteCategory> children;
    /** 用户信息 */
    private User user;

    // 构造方法
    public NoteCategory() {
        this.color = "#409EFF";
        this.icon = "folder";
        this.sortOrder = 0;
        this.noteCount = 0;
    }

    // Getter和Setter方法
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getNoteCount() {
        return noteCount;
    }

    public void setNoteCount(Integer noteCount) {
        this.noteCount = noteCount;
    }

    public LocalDateTime getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(LocalDateTime createdTime) {
        this.createdTime = createdTime;
    }

    public LocalDateTime getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(LocalDateTime updatedTime) {
        this.updatedTime = updatedTime;
    }

    public NoteCategory getParent() {
        return parent;
    }

    public void setParent(NoteCategory parent) {
        this.parent = parent;
    }

    public List<NoteCategory> getChildren() {
        return children;
    }

    public void setChildren(List<NoteCategory> children) {
        this.children = children;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "NoteCategory{" +
                "id=" + id +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", color='" + color + '\'' +
                ", icon='" + icon + '\'' +
                ", parentId=" + parentId +
                ", sortOrder=" + sortOrder +
                ", noteCount=" + noteCount +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                '}';
    }
}