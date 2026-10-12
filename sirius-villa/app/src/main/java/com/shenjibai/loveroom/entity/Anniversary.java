package com.shenjibai.loveroom.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Lira - 纪念日实体
 * 默认 11-22 沈既白生日
 * 署名: Lira
 */
@Entity(tableName = "anniversaries")
public class Anniversary {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;

    // 格式 yyyy-MM-dd
    private String date;

    public Anniversary() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
