package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ActionLog
import com.example.data.model.ChatMessage
import com.example.data.model.Memory
import com.example.data.model.Reminder

@Database(
    entities = [
        Memory::class,
        Reminder::class,
        ActionLog::class,
        ChatMessage::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun actionLogDao(): ActionLogDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sweaty_ai.db"
                ).addCallback(object : Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Guarantee full UTF-8 encoding support for Bengali characters
                        db.execSQL("PRAGMA encoding = 'UTF-8';")
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
