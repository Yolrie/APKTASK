package com.example.apktask;

public class Task {
    private String id;
    private String title;
    private boolean isDone;
    private long createAt;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public boolean getIsDone() {
        return isDone;
    }
    public void setIsDone(boolean isDone) {
        this.isDone = isDone;
    }
    public long getCreateAt() {
        return createAt;
    }
    public void setCreateAt(long createAt) {
        this.createAt = createAt;
    }
    public Task (String id, String title, boolean isDone, long createAt) {
        this.id = id;
        this.title = title;
        this.isDone = isDone;
        this.createAt = createAt;
    }
}