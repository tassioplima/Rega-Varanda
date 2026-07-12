package com.tassiolima.regavaranda.ui.plant

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tassiolima.regavaranda.data.local.AnalysisStatus
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.PlantPhotoEntity
import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.di.ServiceLocator
import com.tassiolima.regavaranda.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlantDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepo = ServiceLocator.plantRepository(application)
    private val photoRepo = ServiceLocator.plantPhotoRepository(application)
    private val apiKeyRepo = ServiceLocator.apiKeyRepository(application)

    private val _plant = MutableStateFlow<PlantEntity?>(null)
    val plant: StateFlow<PlantEntity?> = _plant.asStateFlow()

    private val _photos = MutableStateFlow<List<PlantPhotoEntity>>(emptyList())
    val photos: StateFlow<List<PlantPhotoEntity>> = _photos.asStateFlow()

    private var loadedPlantId: Long = -1

    fun load(plantId: Long) {
        if (loadedPlantId == plantId) return
        loadedPlantId = plantId
        viewModelScope.launch {
            _plant.value = plantRepo.getPlant(plantId)
        }
        viewModelScope.launch {
            photoRepo.observeForPlant(plantId).collectLatest { _photos.value = it }
        }
    }

    fun prepareNewPhotoFile(context: Context, plantId: Long): Pair<File, Uri> {
        val file = ImageUtils.newPhotoFile(context, plantId)
        val uri = ImageUtils.uriForFile(context, file)
        return file to uri
    }

    fun onPhotoCaptured(plantId: Long, file: File) {
        viewModelScope.launch { saveAndAnalyze(plantId, file) }
    }

    fun onGalleryPhotoPicked(context: Context, plantId: Long, uri: Uri) {
        viewModelScope.launch {
            val file = ImageUtils.newPhotoFile(context, plantId)
            val copied = withContext(Dispatchers.IO) { ImageUtils.copyUriToFile(context, uri, file) }
            if (!copied) return@launch
            saveAndAnalyze(plantId, file)
        }
    }

    private suspend fun saveAndAnalyze(plantId: Long, file: File) {
        withContext(Dispatchers.IO) { ImageUtils.normalizeInPlace(file) }
        val photo = PlantPhotoEntity(
            plantId = plantId,
            filePath = file.absolutePath,
            takenAt = System.currentTimeMillis()
        )
        val id = photoRepo.insert(photo)
        analyze(id)
    }

    fun analyze(photoId: Long) {
        viewModelScope.launch {
            val photo = photoRepo.getById(photoId) ?: return@launch
            val plant = _plant.value ?: return@launch
            val provider = apiKeyRepo.getSelectedProvider()
            val apiKey = apiKeyRepo.getKey(provider)
            if (apiKey.isNullOrBlank()) {
                photoRepo.update(photo.copy(analysisStatus = AnalysisStatus.NO_API_KEY))
                return@launch
            }

            photoRepo.update(photo.copy(analysisStatus = AnalysisStatus.ANALYZING))
            try {
                val base64 = withContext(Dispatchers.IO) { ImageUtils.toBase64Jpeg(File(photo.filePath)) }
                val historySummary = buildHistorySummary(plant.id, excludingPhotoId = photo.id)
                val visionClient = ServiceLocator.visionClientFor(provider)
                val result = visionClient.analyzePhoto(
                    apiKey,
                    base64,
                    plant.name,
                    plant.category,
                    plant.notes,
                    historySummary
                )
                photoRepo.update(
                    photo.copy(
                        analysisStatus = AnalysisStatus.DONE,
                        aiSuggestedHealth = result.healthState,
                        diagnosis = result.diagnosis,
                        wateringTip = result.wateringTip,
                        pruningTip = result.pruningTip,
                        fertilizingTip = result.fertilizingTip,
                        repottingTip = result.repottingTip,
                        identifiedSpecies = result.identifiedSpecies.takeIf { it.isNotBlank() },
                        recommendedWateringIntervalDays = result.recommendedWateringIntervalDays,
                        evolutionNote = result.evolutionNote.takeIf { it.isNotBlank() }
                    )
                )

                if (result.identifiedSpecies.isNotBlank() || result.recommendedWateringIntervalDays != null) {
                    val latestPlant = plantRepo.getPlant(plant.id) ?: plant
                    plantRepo.savePlant(
                        latestPlant.copy(
                            identifiedSpecies = result.identifiedSpecies.takeIf { it.isNotBlank() } ?: latestPlant.identifiedSpecies,
                            aiWateringIntervalDays = result.recommendedWateringIntervalDays ?: latestPlant.aiWateringIntervalDays
                        )
                    )
                    _plant.value = plantRepo.getPlant(plant.id)
                }
            } catch (e: Exception) {
                photoRepo.update(photo.copy(analysisStatus = AnalysisStatus.FAILED, diagnosis = e.message))
            }
        }
    }

    /** Resumo cronológico das análises anteriores desta planta, para dar contexto de evolução à IA. */
    private suspend fun buildHistorySummary(plantId: Long, excludingPhotoId: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val previousPhotos = photoRepo.observeForPlant(plantId).first()
            .filter { it.id != excludingPhotoId && it.analysisStatus == AnalysisStatus.DONE }
            .sortedBy { it.takenAt }
            .takeLast(5)

        if (previousPhotos.isEmpty()) return ""

        return previousPhotos.joinToString("\n") { photo ->
            val date = dateFormat.format(Date(photo.takenAt))
            val health = photo.confirmedHealth?.label ?: photo.aiSuggestedHealth?.label ?: "?"
            "- $date: $health — ${photo.diagnosis.orEmpty()}"
        }
    }

    fun confirmHealth(photo: PlantPhotoEntity, state: HealthState) {
        viewModelScope.launch { photoRepo.update(photo.copy(confirmedHealth = state)) }
    }

    fun deletePhoto(photo: PlantPhotoEntity) {
        viewModelScope.launch {
            photoRepo.delete(photo)
            runCatching { File(photo.filePath).delete() }
        }
    }

    fun recordMoistureReading(level: Int) {
        val current = _plant.value ?: return
        viewModelScope.launch {
            plantRepo.recordMoistureReading(current, level, System.currentTimeMillis())
            _plant.value = plantRepo.getPlant(current.id)
        }
    }
}
