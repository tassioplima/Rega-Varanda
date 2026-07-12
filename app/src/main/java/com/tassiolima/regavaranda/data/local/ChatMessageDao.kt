package com.tassiolima.regavaranda.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE plantId = :plantId ORDER BY createdAt ASC")
    fun observeForPlant(plantId: Long): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE plantId = :plantId ORDER BY createdAt ASC")
    suspend fun getForPlant(plantId: Long): List<ChatMessageEntity>

    @Insert
    suspend fun insert(message: ChatMessageEntity): Long
}
