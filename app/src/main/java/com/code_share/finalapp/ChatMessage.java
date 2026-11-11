package com.code_share.finalapp;

public class ChatMessage {
    private String sender;
    private String messageText;
    private boolean isAI;

    public ChatMessage(String sender, String Message, boolean isAI) {
        this.sender = sender;
        this.messageText = Message;
        this.isAI = isAI;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return messageText;
    }

    public boolean isAI() {
        return isAI;
    }
} 




