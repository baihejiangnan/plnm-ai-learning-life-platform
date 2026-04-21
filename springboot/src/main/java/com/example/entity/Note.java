package com.example.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 笔记实体类
 */
public class Note {

    /** 笔记ID */
    private Long id;
    /** 用户ID */
    private Integer userId;
    /** 笔记标题 */
    private String title;
    /** 笔记内容(Markdown格式) */
    private String content;
    /** 内容类型：markdown, html, text */
    private String contentType;
    /** 分类ID */
    private Integer categoryId;
    /** 文件夹路径 */
    private String folderPath;
    /** 是否收藏：1-是，0-否 */
    private Boolean isFavorite;
    /** 是否置顶：1-是，0-否 */
    private Boolean isPinned;
    /** 是否公开：1-公开，0-私有 */
    private Boolean isPublic;
    /** 查看次数 */
    private Integer viewCount;
    /** 字数统计 */
    private Integer wordCount;
    /** 状态：1-正常，0-草稿，-1-删除 */
    private Integer status;
    /** 版本号 */
    private Integer version;
    /** 创建时间 */
    private LocalDateTime createdTime;
    /** 更新时间 */
    private LocalDateTime updatedTime;
    /** 最后查看时间 */
    private LocalDateTime lastViewedTime;

    // 关联对象
    /** 分类信息 */
    private NoteCategory category;
    /** 标签列表 */
    private List<Tag> tags;
    /** 用户信息 */
    private User user;

    // 构造方法
    public Note() {
        this.contentType = "markdown";
        this.isFavorite = false;
        this.isPinned = false;
        this.isPublic = false;
        this.viewCount = 0;
        this.wordCount = 0;
        this.status = 1;
        this.version = 1;
    }

    // Getter和Setter方法
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    public Boolean getIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(Boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public Boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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

    public LocalDateTime getLastViewedTime() {
        return lastViewedTime;
    }

    public void setLastViewedTime(LocalDateTime lastViewedTime) {
        this.lastViewedTime = lastViewedTime;
    }

    public NoteCategory getCategory() {
        return category;
    }

    public void setCategory(NoteCategory category) {
        this.category = category;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", userId=" + userId +
                ", title='" + title + '\'' +
                ", contentType='" + contentType + '\'' +
                ", categoryId=" + categoryId +
                ", isFavorite=" + isFavorite +
                ", isPinned=" + isPinned +
                ", isPublic=" + isPublic +
                ", viewCount=" + viewCount +
                ", wordCount=" + wordCount +
                ", status=" + status +
                ", version=" + version +
                ", createdTime=" + createdTime +
                ", updatedTime=" + updatedTime +
                '}';
    }
}