package com.shenjibai.loveroom.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.shenjibai.loveroom.dao.AlbumPhotoDao;
import com.shenjibai.loveroom.dao.AnniversaryDao;
import com.shenjibai.loveroom.dao.ChatMessageDao;
import com.shenjibai.loveroom.dao.DiaryDao;
import com.shenjibai.loveroom.dao.MemoryDao;
import com.shenjibai.loveroom.dao.PersonaDao;
import com.shenjibai.loveroom.entity.AlbumPhoto;
import com.shenjibai.loveroom.entity.Anniversary;
import com.shenjibai.loveroom.entity.ChatMessage;
import com.shenjibai.loveroom.entity.Diary;
import com.shenjibai.loveroom.entity.Memory;
import com.shenjibai.loveroom.entity.Persona;

/**
 * Lira - Room 数据库
 * 署名: Lira
 */
@Database(entities = {
        ChatMessage.class,
        Persona.class,
        Memory.class,
        Diary.class,
        Anniversary.class,
        AlbumPhoto.class
}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract ChatMessageDao chatMessageDao();
    public abstract PersonaDao personaDao();
    public abstract MemoryDao memoryDao();
    public abstract DiaryDao diaryDao();
    public abstract AnniversaryDao anniversaryDao();
    public abstract AlbumPhotoDao albumPhotoDao();

    public static AppDatabase getLira(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "sirius_villa_db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
