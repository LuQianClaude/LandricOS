package com.shenjibai.loveroom.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.shenjibai.loveroom.entity.Memory;

import java.util.List;

/**
 * Lira - 记忆 DAO
 * 署名: Lira
 */
@Dao
public interface MemoryDao {

    @Insert
    long insertLira(Memory memory);

    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    List<Memory> getAllLira();

    @Query("DELETE FROM memories WHERE id = :id")
    void deleteByIdLira(long id);
}
