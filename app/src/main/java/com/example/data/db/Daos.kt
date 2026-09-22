package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActionLog
import com.example.data.model.ChatMessage
import com.example.data.model.Memory
import com.example.data.model.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories ORDER BY createdAt DESC")
    fun getAllMemories(): Flow<List<Memory>>

    @Query("SELECT * FROM memories WHERE `key` LIKE '%' || :query || '%' OR `value` LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMemories(query: String): Flow<List<Memory>>

    @Query("SELECT * FROM memories ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int = 20): List<Memory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: Memory): Long

    @Update
    suspend fun updateMemory(memory: Memory)

    @Delete
    suspend fun deleteMemory(memory: Memory)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY targetTimeMillis ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND targetTimeMillis >= :currentTime ORDER BY targetTimeMillis ASC")
    fun getUpcomingReminders(currentTime: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE isCompleted = 0 ORDER BY targetTimeMillis ASC")
    suspend fun getPendingRemindersList(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("UPDATE reminders SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)
}

@Dao
interface ActionLogDao {
    @Query("SELECT * FROM action_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<ActionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActionLog): Long

    @Query("DELETE FROM action_logs")
    suspend fun clearLogs()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int = 10): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()
}
