package com.tassiolima.regavaranda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tassiolima.regavaranda.data.model.ChatRole

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val role: ChatRole,
    val content: String,
    val createdAt: Long
)
