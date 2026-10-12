package com.shenjibai.loveroom.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Lira - 长期记忆片段实体
 * 署名: Lira
 */
@Entity(tableName = "memories")
public class Memory {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String content;
    private long createdAt;

    public Memory() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
