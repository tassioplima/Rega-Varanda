package com.tassiolima.regavaranda.ui.plant

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.PlantPhotoEntity
import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.di.ServiceLocator
import com.tassiolima.regavaranda.util.ImageUtils
import com.tassiolima.regavaranda.util.PhotoAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PlantDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepo = ServiceLocator.plantRepository(application)
    private val photoRepo = ServiceLocator.plantPhotoRepository(application)
    private val photoAnalyzer = PhotoAnalyzer(application)

    private val _plant = MutableStateFlow<PlantEntity?>(null)
    val plant: StateFlow<PlantEntity?> = _plant.asStateFlow()

    private val _photos = MutableStateFlow<List<PlantPhotoEntity>>(emptyList())
    val photos: StateFlow<List<PlantPhotoEntity>> = _photos.asStateFlow()

    private var loadedPlantId: Long = -1

    fun load(plantId: Long) {
        if (loadedPlantId == plantId) return
        loadedPlantId = plantId
        // Observa o banco em vez de ler uma vez: assim edições feitas em outra tela
        // (nome, orientação) aparecem aqui imediatamente ao voltar.
        viewModelScope.launch {
            plantRepo.observePlant(plantId).collectLatest { _plant.value = it }
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
        viewModelScope.launch { photoAnalyzer.analyze(photoId) }
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
        }
    }
}
