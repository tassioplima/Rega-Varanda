package com.tassiolima.regavaranda.data.remote

import com.tassiolima.regavaranda.data.model.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ClaudeChatClient(private val client: OkHttpClient = OkHttpClient()) : AiChatClient {

    override suspend fun sendMessage(
        apiKey: String,
        systemPrompt: String,
        history: List<ChatTurn>,
        newUserMessage: String
    ): String = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        history.forEach { turn ->
            messages.put(
                JSONObject().apply {
                    put("role", if (turn.role == ChatRole.USER) "user" else "assistant")
                    put("content", turn.content)
                }
            )
        }
        messages.put(JSONObject().apply { put("role", "user"); put("content", newUserMessage) })

        val body = JSONObject().apply {
            put("model", "claude-sonnet-5")
            put("max_tokens", 1024)
            put("system", systemPrompt)
            put("messages", messages)
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(extractErrorMessage(bodyText, response.code))
            }
            parseResponse(bodyText)
        }
    }

    private fun parseResponse(body: String): String {
        val contentArray = JSONObject(body).getJSONArray("content")
        return (0 until contentArray.length())
            .map { contentArray.getJSONObject(it) }
            .firstOrNull { it.optString("type") == "text" }
            ?.optString("text")
            ?.trim()
            ?: throw IOException("Resposta da IA sem conteúdo de texto")
    }

    private fun extractErrorMessage(body: String, code: Int): String =
        runCatching { JSONObject(body).getJSONObject("error").getString("message") }
            .getOrDefault("Erro na API da Anthropic (HTTP $code)")
}
