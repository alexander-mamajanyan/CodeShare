package com.code_share.finalapp;

public class UserScore {
    private String userId;
    private String username;
    private int score;
    private String photoUrl;

    public UserScore(String userId, String username, int score) {
        this.userId = userId;
        this.username = username;
        this.score = score;
        this.photoUrl = null;
    }
    
    public UserScore(String userId, String username, int score, String photoUrl) {
        this.userId = userId;
        this.username = username;
        this.score = score;
        this.photoUrl = photoUrl;
    }

    public String getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }

    public int getScore() {
        return score;
    }
    
    public String getPhotoUrl() {
        return photoUrl;
    }
    
    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}




