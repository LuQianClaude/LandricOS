package com.shenjibai.loveroom.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Lira - 相册图片备注实体
 * assetPath 对应 assets/album 下的文件名
 * 署名: Lira
 */
@Entity(tableName = "album_photos")
public class AlbumPhoto {

    @PrimaryKey(autoGenerate = true)
    private long id;

    // assets/album 中的相对路径
    private String assetPath;

    private String note;

    public AlbumPhoto() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getAssetPath() { return assetPath; }
    public void setAssetPath(String assetPath) { this.assetPath = assetPath; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
