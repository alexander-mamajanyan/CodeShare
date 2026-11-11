package com.code_share.finalapp;

public class Lesson {
    private String title;
    private String description;
    private String content;
    private String htmlFile;
    private boolean available;
    private String topics;

    public Lesson(String title, String description, String content, String htmlFile, boolean available, String topics) {
        this.title = title;
        this.description = description;
        this.content = content;
        this.htmlFile = htmlFile;
        this.available = available;
        this.topics = topics;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }

    public String getHtmlFile() {
        return htmlFile;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getTopics() {
        return topics;
    }
} 




