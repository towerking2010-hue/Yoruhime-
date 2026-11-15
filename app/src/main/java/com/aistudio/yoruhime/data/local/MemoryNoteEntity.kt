package com.aistudio.yoruhime.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_notes")
data class MemoryNoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val category: String = "general",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val importance: Int = 2
)
