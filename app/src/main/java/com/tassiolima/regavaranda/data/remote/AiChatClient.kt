package com.tassiolima.regavaranda.data.remote

import com.tassiolima.regavaranda.data.model.ChatRole

data class ChatTurn(val role: ChatRole, val content: String)

interface AiChatClient {
    /**
     * Envia o histórico da conversa (sem a última mensagem do usuário incluída em [history])
     * junto com a nova pergunta, e retorna a resposta em texto livre da IA.
     */
    suspend fun sendMessage(apiKey: String, systemPrompt: String, history: List<ChatTurn>, newUserMessage: String): String
}
