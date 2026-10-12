package com.shenjibai.loveroom.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.shenjibai.loveroom.entity.ChatMessage;

import java.util.List;

/**
 * Lira - 聊天消息 DAO
 * 署名: Lira
 */
@Dao
public interface ChatMessageDao {

    @Insert
    long insertLira(ChatMessage message);

    @Update
    void updateLira(ChatMessage message);

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    LiveData<List<ChatMessage>> getAllLiveLira();

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    List<ChatMessage> getAllLira();

    @Query("SELECT * FROM chat_messages WHERE important = 1 ORDER BY timestamp DESC")
    List<ChatMessage> getImportantLira();

    @Query("DELETE FROM chat_messages WHERE id = :id")
    void deleteByIdLira(long id);

    @Query("SELECT * FROM chat_messages WHERE id = :id")
    ChatMessage getByIdLira(long id);
}
