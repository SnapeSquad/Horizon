package com.horizon.launcher.models;

import java.util.Date;

/**
 * Модель сообщения форума
 */
public class ForumPost {
    private int id;
    private int topicId;
    private String authorUsername;
    private UserRole authorRole;
    private String content;
    private boolean isEdited;
    private Date editedAt;
    private Date createdAt;
    private int likesCount;
    private boolean isLiked;
    
    public ForumPost() {
    }
    
    // Getters and Setters
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getTopicId() {
        return topicId;
    }
    
    public void setTopicId(int topicId) {
        this.topicId = topicId;
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
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public boolean isEdited() {
        return isEdited;
    }
    
    public void setEdited(boolean edited) {
        isEdited = edited;
    }
    
    public Date getEditedAt() {
        return editedAt;
    }
    
    public void setEditedAt(Date editedAt) {
        this.editedAt = editedAt;
    }
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public int getLikesCount() {
        return likesCount;
    }
    
    public void setLikesCount(int likesCount) {
        this.likesCount = likesCount;
    }
    
    public boolean isLiked() {
        return isLiked;
    }
    
    public void setLiked(boolean liked) {
        isLiked = liked;
    }
}
