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

class GeminiChatClient(private val client: OkHttpClient = OkHttpClient()) : AiChatClient {

    override suspend fun sendMessage(
        apiKey: String,
        systemPrompt: String,
        history: List<ChatTurn>,
        newUserMessage: String
    ): String = withContext(Dispatchers.IO) {
        val contents = JSONArray()
        history.forEach { turn ->
            contents.put(
                JSONObject().apply {
                    put("role", if (turn.role == ChatRole.USER) "user" else "model")
                    put("parts", JSONArray().put(JSONObject().apply { put("text", turn.content) }))
                }
            )
        }
        contents.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().apply { put("text", newUserMessage) }))
            }
        )

        val body = JSONObject().apply {
            put("contents", contents)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply { put("text", systemPrompt) }))
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
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
        val text = JSONObject(body)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .optString("text")
        if (text.isBlank()) throw IOException("Resposta da IA sem conteúdo de texto")
        return text.trim()
    }

    private fun extractErrorMessage(body: String, code: Int): String =
        runCatching { JSONObject(body).getJSONObject("error").getString("message") }
            .getOrDefault("Erro na API do Google Gemini (HTTP $code)")

    companion object {
        private const val MODEL = "gemini-3.5-flash"
    }
}
