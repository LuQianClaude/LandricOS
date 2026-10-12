package com.shenjibai.loveroom.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.shenjibai.loveroom.entity.Persona;

/**
 * Lira - 人设 DAO
 * 署名: Lira
 */
@Dao
public interface PersonaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLira(Persona persona);

    @Update
    void updateLira(Persona persona);

    @Query("SELECT * FROM persona LIMIT 1")
    Persona getLira();
}
