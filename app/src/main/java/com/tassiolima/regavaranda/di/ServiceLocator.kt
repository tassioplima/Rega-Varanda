package com.tassiolima.regavaranda.di

import android.content.Context
import com.tassiolima.regavaranda.data.local.AppDatabase
import com.tassiolima.regavaranda.data.location.LocationProvider
import com.tassiolima.regavaranda.data.model.AiProvider
import com.tassiolima.regavaranda.data.remote.AiChatClient
import com.tassiolima.regavaranda.data.remote.ClaudeChatClient
import com.tassiolima.regavaranda.data.remote.ClaudeVisionClient
import com.tassiolima.regavaranda.data.remote.GeminiChatClient
import com.tassiolima.regavaranda.data.remote.GeminiVisionClient
import com.tassiolima.regavaranda.data.remote.OpenMeteoClient
import com.tassiolima.regavaranda.data.remote.VisionAnalysisClient
import com.tassiolima.regavaranda.data.repository.ApiKeyRepository
import com.tassiolima.regavaranda.data.repository.ChatRepository
import com.tassiolima.regavaranda.data.repository.PlantPhotoRepository
import com.tassiolima.regavaranda.data.repository.PlantRepository
import com.tassiolima.regavaranda.data.repository.SettingsRepository
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient

object ServiceLocator {
    @Volatile private var plantRepository: PlantRepository? = null
    @Volatile private var plantPhotoRepository: PlantPhotoRepository? = null
    @Volatile private var chatRepository: ChatRepository? = null
    @Volatile private var settingsRepository: SettingsRepository? = null
    @Volatile private var apiKeyRepository: ApiKeyRepository? = null
    @Volatile private var locationProvider: LocationProvider? = null
    private val weatherClient by lazy { OpenMeteoClient() }

    /**
     * Chat/vision calls enviam fotos e pedem respostas longas da IA, o que costuma passar
     * dos 10s padrão do OkHttp — daí os timeouts maiores para conexão/leitura/escrita.
     */
    private val aiHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .build()
    }
    private val claudeVisionClient by lazy { ClaudeVisionClient(aiHttpClient) }
    private val geminiVisionClient by lazy { GeminiVisionClient(aiHttpClient) }
    private val claudeChatClient by lazy { ClaudeChatClient(aiHttpClient) }
    private val geminiChatClient by lazy { GeminiChatClient(aiHttpClient) }

    fun plantRepository(context: Context): PlantRepository =
        plantRepository ?: synchronized(this) {
            plantRepository ?: PlantRepository(
                AppDatabase.getInstance(context).plantDao(),
                AppDatabase.getInstance(context).wateringLogDao()
            ).also { plantRepository = it }
        }

    fun plantPhotoRepository(context: Context): PlantPhotoRepository =
        plantPhotoRepository ?: synchronized(this) {
            plantPhotoRepository ?: PlantPhotoRepository(AppDatabase.getInstance(context).plantPhotoDao())
                .also { plantPhotoRepository = it }
        }

    fun chatRepository(context: Context): ChatRepository =
        chatRepository ?: synchronized(this) {
            chatRepository ?: ChatRepository(AppDatabase.getInstance(context).chatMessageDao())
                .also { chatRepository = it }
        }

    fun settingsRepository(context: Context): SettingsRepository =
        settingsRepository ?: synchronized(this) {
            settingsRepository ?: SettingsRepository(context.applicationContext)
                .also { settingsRepository = it }
        }

    fun apiKeyRepository(context: Context): ApiKeyRepository =
        apiKeyRepository ?: synchronized(this) {
            apiKeyRepository ?: ApiKeyRepository(context.applicationContext)
                .also { apiKeyRepository = it }
        }

    fun locationProvider(context: Context): LocationProvider =
        locationProvider ?: synchronized(this) {
            locationProvider ?: LocationProvider(context.applicationContext)
                .also { locationProvider = it }
        }

    fun weatherClient(): OpenMeteoClient = weatherClient

    fun claudeVisionClient(): ClaudeVisionClient = claudeVisionClient

    fun geminiVisionClient(): GeminiVisionClient = geminiVisionClient

    fun visionClientFor(provider: AiProvider): VisionAnalysisClient = when (provider) {
        AiProvider.ANTHROPIC -> claudeVisionClient
        AiProvider.GEMINI -> geminiVisionClient
    }

    fun chatClientFor(provider: AiProvider): AiChatClient = when (provider) {
        AiProvider.ANTHROPIC -> claudeChatClient
        AiProvider.GEMINI -> geminiChatClient
    }
}
