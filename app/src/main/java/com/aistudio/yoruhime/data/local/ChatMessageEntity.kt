package com.aistudio.yoruhime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val text: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val emotionTag: String = "normal", // smile, blush, happy, sad, angry, normal
    val isOffline: Boolean = false
)
