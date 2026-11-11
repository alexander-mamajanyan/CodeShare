package com.code_share.finalapp;

public class FriendRequest {
    private String requestId;
    private String senderId;
    private String senderName;
    private long timestamp;

    public FriendRequest() {
        // Required empty constructor for Firebase
    }

    public FriendRequest(String requestId, String senderId, String senderName) {
        this.requestId = requestId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
    
    public String getName() {
        return senderName;
    }
    
    public void setId(String id) {
        this.requestId = id;
    }
    
    public String getId() {
        return requestId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
} 




