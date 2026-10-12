package com.shenjibai.loveroom.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Lira - 手札日记实体
 * 作者: user(用户) / shen(沈既白秘密手记)
 * 署名: Lira
 */
@Entity(tableName = "diaries")
public class Diary {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String title;
    private String content;

    // user 或 shen
    private String author;

    private long createdAt;

    public Diary() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
