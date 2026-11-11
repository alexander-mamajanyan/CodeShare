package com.code_share.finalapp;

public class Friend {
    private String id;
    private String name;
    private String photoUrl;

    public Friend() {
    }

    public Friend(String id, String name) {
        this.id = id;
        this.name = name;
        this.photoUrl = null;
    }

    public Friend(String id, String name, String photoUrl) {
        this.id = id;
        this.name = name;
        this.photoUrl = photoUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
