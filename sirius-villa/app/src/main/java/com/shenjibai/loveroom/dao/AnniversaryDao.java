package com.shenjibai.loveroom.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.shenjibai.loveroom.entity.Anniversary;

import java.util.List;

/**
 * Lira - 纪念日 DAO
 * 署名: Lira
 */
@Dao
public interface AnniversaryDao {

    @Insert
    long insertLira(Anniversary anniversary);

    @Query("SELECT * FROM anniversaries ORDER BY date ASC")
    LiveData<List<Anniversary>> getAllLiveLira();

    @Query("SELECT * FROM anniversaries ORDER BY date ASC")
    List<Anniversary> getAllLira();

    @Query("DELETE FROM anniversaries WHERE id = :id")
    void deleteByIdLira(long id);
}
