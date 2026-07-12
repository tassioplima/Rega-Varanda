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

class GeminiVisionClient(private val client: OkHttpClient = OkHttpClient()) : VisionAnalysisClient {

    override suspend fun analyzePhoto(
        apiKey: String,
        imageBase64Jpeg: String,
        plantName: String,
        category: PlantCategory,
        userNotes: String,
        historySummary: String
    ): PhotoAnalysisResult = withContext(Dispatchers.IO) {
        val requestBody = buildRequestBody(imageBase64Jpeg, plantName, category, userNotes, historySummary)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
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
        val parts = JSONArray()
            .put(JSONObject().apply { put("text", VisionPrompt.build(plantName, category, userNotes, historySummary)) })
            .put(
                JSONObject().apply {
                    put(
                        "inline_data",
                        JSONObject().apply {
                            put("mime_type", "image/jpeg")
                            put("data", imageBase64Jpeg)
                        }
                    )
                }
            )

        return JSONObject().apply {
            put(
                "contents",
                JSONArray().put(JSONObject().apply { put("parts", parts) })
            )
            put(
                "generationConfig",
                JSONObject().apply { put("response_mime_type", "application/json") }
            )
        }
    }

    private fun parseResponse(body: String): PhotoAnalysisResult {
        val json = JSONObject(body)
        val text = json.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .optString("text")

        if (text.isBlank()) throw IOException("Resposta da IA sem conteúdo de texto")
        return VisionPrompt.parse(text)
    }

    private fun extractErrorMessage(body: String, code: Int): String =
        runCatching { JSONObject(body).getJSONObject("error").getString("message") }
            .getOrDefault("Erro na API do Google Gemini (HTTP $code)")

    companion object {
        private const val MODEL = "gemini-3.5-flash"
    }
}
