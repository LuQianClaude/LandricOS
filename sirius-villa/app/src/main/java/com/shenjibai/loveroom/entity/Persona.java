package com.shenjibai.loveroom.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Lira - 世界书/人设实体
 * 由用户填写，不硬编码任何角色内容
 * 署名: Lira
 */
@Entity(tableName = "persona")
public class Persona {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String name;
    private String appearance;
    private String personality;
    private String background;
    private String speechStyle;

    // 自定义 System Prompt，每次 API 请求自动携带
    private String systemPrompt;

    private long updatedAt;

    public Persona() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAppearance() { return appearance; }
    public void setAppearance(String appearance) { this.appearance = appearance; }

    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }

    public String getBackground() { return background; }
    public void setBackground(String background) { this.background = background; }

    public String getSpeechStyle() { return speechStyle; }
    public void setSpeechStyle(String speechStyle) { this.speechStyle = speechStyle; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
