package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class Memory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val value: String,
    val category: String = "general",
    val language: String = "en",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetTimeMillis: Long,
    val isCompleted: Boolean = false,
    val source: String = "voice",
    val language: String = "en",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "action_logs")
data class ActionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String,
    val details: String,
    val success: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val isUser: Boolean,
    val language: String = "en",
    val actionJson: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
