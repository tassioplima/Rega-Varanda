package com.tassiolima.regavaranda.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tassiolima.regavaranda.data.local.AnalysisStatus
import com.tassiolima.regavaranda.data.local.ChatMessageEntity
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.PlantPhotoEntity
import com.tassiolima.regavaranda.data.model.ChatRole
import com.tassiolima.regavaranda.data.remote.ChatTurn
import com.tassiolima.regavaranda.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantChatViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepo = ServiceLocator.plantRepository(application)
    private val photoRepo = ServiceLocator.plantPhotoRepository(application)
    private val chatRepo = ServiceLocator.chatRepository(application)
    private val apiKeyRepo = ServiceLocator.apiKeyRepository(application)

    private var loadedPlantId: Long = -1
    private var systemPrompt: String = ""

    private val _plant = MutableStateFlow<PlantEntity?>(null)
    val plant: StateFlow<PlantEntity?> = _plant.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    companion object {
        private const val MAX_CHAT_HISTORY_MESSAGES = 24
    }

    fun load(plantId: Long) {
        if (loadedPlantId == plantId) return
        loadedPlantId = plantId
        viewModelScope.launch {
            val plant = plantRepo.getPlant(plantId)
            _plant.value = plant
            val photos = photoRepo.observeForPlant(plantId).first()
            val analyzed = photos.filter { it.analysisStatus == AnalysisStatus.DONE }.sortedBy { it.takenAt }
            systemPrompt = buildSystemPrompt(plant, analyzed)
        }
        viewModelScope.launch {
            chatRepo.observeForPlant(plantId).collectLatest { _messages.value = it }
        }
    }

    private fun buildSystemPrompt(plant: PlantEntity?, analyzedPhotosChronological: List<PlantPhotoEntity>): String {
        if (plant == null) return "Você é um especialista em jardinagem e cuidado de plantas."

        val notesLine = plant.notes.takeIf { it.isNotBlank() }?.let { "Observações do dono sobre a planta: $it." } ?: ""

        val latestPhoto = analyzedPhotosChronological.lastOrNull()
        val tipsLine = latestPhoto?.let { photo ->
            listOfNotNull(
                photo.wateringTip?.takeIf { it.isNotBlank() }?.let { "rega: $it" },
                photo.pruningTip?.takeIf { it.isNotBlank() }?.let { "poda: $it" },
                photo.fertilizingTip?.takeIf { it.isNotBlank() }?.let { "adubação: $it" },
                photo.repottingTip?.takeIf { it.isNotBlank() }?.let { "vaso: $it" }
            ).takeIf { it.isNotEmpty() }?.joinToString("; ")?.let { "Últimas recomendações: $it." }
        } ?: ""

        val historyBlock = if (analyzedPhotosChronological.isNotEmpty()) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
            val historyLines = analyzedPhotosChronological.takeLast(8).joinToString("\n") { photo ->
                val date = dateFormat.format(Date(photo.takenAt))
                val health = photo.confirmedHealth?.label ?: photo.aiSuggestedHealth?.label ?: "?"
                "- $date: $health — ${photo.diagnosis.orEmpty()}"
            }
            """
            Histórico de fotos e diagnósticos desta planta ao longo do tempo (da mais antiga para a mais
            recente):
            $historyLines

            Use esse histórico para responder perguntas sobre evolução/progresso da planta (se está
            melhorando, piorando ou estável, e por quê), comparando os diagnósticos ao longo do tempo.
            """.trimIndent()
        } else {
            "Ainda não há fotos analisadas desta planta, então não há histórico de evolução para comparar."
        }

        return """
            Você é um especialista em jardinagem e cuidado de plantas de varanda/apartamento,
            ajudando especificamente com a planta chamada "${plant.name}" (categoria: ${plant.category.label}).
            $notesLine
            $tipsLine

            $historyBlock

            Responda as perguntas do usuário sobre esta planta de forma útil, prática e concisa, em português.
        """.trimIndent()
    }

    fun sendMessage(text: String) {
        val question = text.trim()
        if (question.isBlank() || loadedPlantId == -1L) return
        val plantId = loadedPlantId

        viewModelScope.launch {
            val provider = apiKeyRepo.getSelectedProvider()
            val apiKey = apiKeyRepo.getKey(provider)
            // Só as mensagens mais recentes viram contexto — sem isso, uma conversa antiga
            // reenviaria o histórico inteiro a cada pergunta, deixando a resposta cada vez mais lenta.
            val history = chatRepo.getForPlant(plantId)
                .takeLast(MAX_CHAT_HISTORY_MESSAGES)
                .map { ChatTurn(it.role, it.content) }

            chatRepo.insert(
                ChatMessageEntity(plantId = plantId, role = ChatRole.USER, content = question, createdAt = System.currentTimeMillis())
            )

            if (apiKey.isNullOrBlank()) {
                chatRepo.insert(
                    ChatMessageEntity(
                        plantId = plantId,
                        role = ChatRole.ASSISTANT,
                        content = "⚠️ Configure sua chave de IA nas Configurações para conversar sobre esta planta.",
                        createdAt = System.currentTimeMillis()
                    )
                )
                return@launch
            }

            _isSending.value = true
            try {
                val client = ServiceLocator.chatClientFor(provider)
                val reply = client.sendMessage(apiKey, systemPrompt, history, question)
                chatRepo.insert(
                    ChatMessageEntity(plantId = plantId, role = ChatRole.ASSISTANT, content = reply, createdAt = System.currentTimeMillis())
                )
            } catch (e: Exception) {
                chatRepo.insert(
                    ChatMessageEntity(
                        plantId = plantId,
                        role = ChatRole.ASSISTANT,
                        content = "⚠️ Erro ao consultar a IA: ${e.message}",
                        createdAt = System.currentTimeMillis()
                    )
                )
            } finally {
                _isSending.value = false
            }
        }
    }
}
