package com.tassiolima.regavaranda.data.remote

import com.tassiolima.regavaranda.data.model.PlantCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ClaudeVisionClient(private val client: OkHttpClient = OkHttpClient()) : VisionAnalysisClient {

    override suspend fun analyzePhoto(
        apiKey: String,
        imageBase64Jpeg: String,
        plantName: String,
        category: PlantCategory,
        userNotes: String,
        historySummary: String
    ): PhotoAnalysisResult = withContext(Dispatchers.IO) {
        val requestBody = buildRequestBody(imageBase64Jpeg, plantName, category, userNotes, historySummary)

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException(extractErrorMessage(bodyText, response.code))
            }
            parseResponse(bodyText)
        }
    }

    private fun buildRequestBody(
        imageBase64Jpeg: String,
        plantName: String,
        category: PlantCategory,
        userNotes: String,
        historySummary: String
    ): JSONObject {
        val content = JSONArray()
            .put(
                JSONObject().apply {
                    put("type", "image")
                    put(
                        "source",
                        JSONObject().apply {
                            put("type", "base64")
                            put("media_type", "image/jpeg")
                            put("data", imageBase64Jpeg)
                        }
                    )
                }
            )
            .put(
                JSONObject().apply {
                    put("type", "text")
                    put("text", VisionPrompt.build(plantName, category, userNotes, historySummary))
                }
            )

        return JSONObject().apply {
            put("model", "claude-sonnet-5")
            put("max_tokens", 1024)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", content)
                    }
                )
            )
        }
    }

    private fun parseResponse(body: String): PhotoAnalysisResult {
        val json = JSONObject(body)
        val contentArray = json.getJSONArray("content")
        val text = (0 until contentArray.length())
            .map { contentArray.getJSONObject(it) }
            .firstOrNull { it.optString("type") == "text" }
            ?.optString("text")
            ?: throw IOException("Resposta da IA sem conteúdo de texto")

        return VisionPrompt.parse(text)
    }

    private fun extractErrorMessage(body: String, code: Int): String =
        runCatching { JSONObject(body).getJSONObject("error").getString("message") }
            .getOrDefault("Erro na API da Anthropic (HTTP $code)")
}
