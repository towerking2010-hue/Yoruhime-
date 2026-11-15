package com.aistudio.yoruhime.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_notes ORDER BY importance DESC, updatedAt DESC")
    fun getAllMemoriesFlow(): Flow<List<MemoryNoteEntity>>

    @Query("SELECT * FROM memory_notes ORDER BY importance DESC, updatedAt DESC LIMIT :limit")
    suspend fun getRelevantMemories(limit: Int): List<MemoryNoteEntity>

    @Query("SELECT * FROM memory_notes WHERE content = :content COLLATE NOCASE LIMIT 1")
    suspend fun findMemory(content: String): MemoryNoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMemory(memory: MemoryNoteEntity): Long

    @Query("UPDATE memory_notes SET content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMemory(id: Long, content: String, updatedAt: Long)

    @Query("DELETE FROM memory_notes WHERE content = :content COLLATE NOCASE")
    suspend fun forgetMemory(content: String)

    @Query("DELETE FROM memory_notes")
    suspend fun clearMemories()
}
