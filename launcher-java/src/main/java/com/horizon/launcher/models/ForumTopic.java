package com.horizon.launcher.models;

import java.util.Date;

/**
 * Модель темы форума
 */
public class ForumTopic {
    private int id;
    private int categoryId;
    private String authorUsername;
    private UserRole authorRole;
    private String title;
    private String content;
    private boolean isPinned;
    private boolean isLocked;
    private int views;
    private int repliesCount;
    private Date lastReplyAt;
    private Date createdAt;
    
    public ForumTopic() {
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
    
    public String getAuthorUsername() {
        return authorUsername;
    }
    
    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }
    
    public UserRole getAuthorRole() {
        return authorRole;
    }
    
    public void setAuthorRole(UserRole authorRole) {
        this.authorRole = authorRole;
    }
    
    public void setAuthorRole(String roleName) {
        this.authorRole = UserRole.fromString(roleName);
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
    
    public boolean isPinned() {
        return isPinned;
    }
    
    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }
    
    public boolean isLocked() {
        return isLocked;
    }
    
    public void setLocked(boolean locked) {
        isLocked = locked;
    }
    
    public int getViews() {
        return views;
    }
    
    public void setViews(int views) {
        this.views = views;
    }
    
    public int getRepliesCount() {
        return repliesCount;
    }
    
    public void setRepliesCount(int repliesCount) {
        this.repliesCount = repliesCount;
    }
    
    public Date getLastReplyAt() {
        return lastReplyAt;
    }
    
    public void setLastReplyAt(Date lastReplyAt) {
        this.lastReplyAt = lastReplyAt;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
