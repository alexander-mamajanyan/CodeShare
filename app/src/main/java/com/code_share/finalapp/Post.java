package com.code_share.finalapp;

import java.util.HashMap;
import java.util.Map;

public class Post {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    private String description;
    private String imageUrl;
    private String codeContent;
    private long timestamp;
    private int likeCount;
    private int commentCount;
    private Map<String, Boolean> likes; // Store User IDs who liked the Post

    // Required empty constructor for Firebase
    public Post() {
        this.likeCount = 0;
        this.commentCount = 0;
        this.likes = new HashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatar() {
        return userAvatar;
    }

    public void setUserAvatar(String userAvatar) {
        this.userAvatar = userAvatar;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCodeContent() {
        return codeContent;
    }

    public void setCodeContent(String codeContent) {
        this.codeContent = codeContent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public Map<String, Boolean> getLikes() {
        return likes != null ? likes : new HashMap<>();
    }

    public void setLikes(Map<String, Boolean> likes) {
        this.likes = likes;
    }

    public boolean isLikedBy(String userId) {
        return likes != null && likes.containsKey(userId);
    }

    // For backward compatibility with ru.myitschool.finalproject.PostAdapter
    public String getContent() {
        return description;
    }

    public void setContent(String content) {
        this.description = content;
    }
} 




