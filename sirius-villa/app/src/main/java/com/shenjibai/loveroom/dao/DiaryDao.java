package com.shenjibai.loveroom.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.shenjibai.loveroom.entity.Diary;

import java.util.List;

/**
 * Lira - 日记 DAO
 * 署名: Lira
 */
@Dao
public interface DiaryDao {

    @Insert
    long insertLira(Diary diary);

    @Query("SELECT * FROM diaries ORDER BY createdAt DESC")
    LiveData<List<Diary>> getAllLiveLira();

    @Query("SELECT * FROM diaries ORDER BY createdAt DESC")
    List<Diary> getAllLira();
}
