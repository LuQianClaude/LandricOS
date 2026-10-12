package com.shenjibai.loveroom.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.shenjibai.loveroom.entity.AlbumPhoto;

import java.util.List;

/**
 * Lira - 相册备注 DAO
 * 署名: Lira
 */
@Dao
public interface AlbumPhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertLira(AlbumPhoto photo);

    @Query("SELECT * FROM album_photos")
    List<AlbumPhoto> getAllLira();

    @Query("SELECT * FROM album_photos WHERE assetPath = :path LIMIT 1")
    AlbumPhoto getByPathLira(String path);
}
