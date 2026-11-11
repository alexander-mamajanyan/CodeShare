package com.code_share.finalapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {
    private String text;
    private String senderId;
    private long timestamp;
    private String type;  // "text" or "code"
    private String exerciseId;  // for code messages
    private String code;  // for code messages

    public Message() {
        // Required empty constructor for Firebase
    }

    public Message(String text, String senderId) {
        this.text = text;
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
        this.type = "text";  // Default type
    }

    public Message(String exerciseTitle, String senderId, String exerciseId, String code) {
        this.text = exerciseTitle;  // Use Exercise title as Message text
        this.senderId = senderId;
        this.timestamp = System.currentTimeMillis();
        this.type = "code";
        this.exerciseId = exerciseId;
        this.code = code;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    public String getType() {
        return type != null ? type : "text";  // Default to text if not set
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
} 




