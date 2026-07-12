package com.tassiolima.regavaranda.data.repository

import com.tassiolima.regavaranda.data.local.ChatMessageDao
import com.tassiolima.regavaranda.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

class ChatRepository(private val dao: ChatMessageDao) {

    fun observeForPlant(plantId: Long): Flow<List<ChatMessageEntity>> = dao.observeForPlant(plantId)

    suspend fun getForPlant(plantId: Long): List<ChatMessageEntity> = dao.getForPlant(plantId)

    suspend fun getAll(): List<ChatMessageEntity> = dao.getAll()

    suspend fun insert(message: ChatMessageEntity): Long = dao.insert(message)
}
