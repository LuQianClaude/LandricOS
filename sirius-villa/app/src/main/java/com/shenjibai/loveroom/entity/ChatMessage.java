package com.shenjibai.loveroom.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Lira - 聊天消息实体
 * 署名: Lira
 */
@Entity(tableName = "chat_messages")
public class ChatMessage {

    @PrimaryKey(autoGenerate = true)
    private long id;

    // 发送者: "shen" 沈既白 / "xiao" 萧渲禾(用户)
    private String sender;

    // 消息类型: text / image / product
    private String type;

    private String content;

    // 图片本地路径或 URL
    private String imagePath;

    // 商品卡片 JSON
    private String productJson;

    private long timestamp;

    // 是否标记为重要长期记忆
    private boolean important;

    // 是否已朗读
    private boolean spoken;

    public ChatMessage() {}

    // ---- getter / setter ----
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public String getProductJson() { return productJson; }
    public void setProductJson(String productJson) { this.productJson = productJson; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isImportant() { return important; }
    public void setImportant(boolean important) { this.important = important; }

    public boolean isSpoken() { return spoken; }
    public void setSpoken(boolean spoken) { this.spoken = spoken; }
}
