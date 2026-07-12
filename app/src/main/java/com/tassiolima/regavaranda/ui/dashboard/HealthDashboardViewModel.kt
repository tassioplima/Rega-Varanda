package com.tassiolima.regavaranda.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.PlantPhotoEntity
import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class HealthTrend { MELHORANDO, PIORANDO, ESTAVEL, DESCONHECIDA }

data class PlantHealthSummary(
    val plant: PlantEntity,
    val latestPhoto: PlantPhotoEntity?,
    val currentHealth: HealthState?,
    val trend: HealthTrend
)

class HealthDashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepo = ServiceLocator.plantRepository(application)
    private val photoRepo = ServiceLocator.plantPhotoRepository(application)

    val summaries: StateFlow<List<PlantHealthSummary>> = combine(
        plantRepo.observePlants(),
        photoRepo.observeAll()
    ) { plants, photos ->
        plants.map { plant ->
            val plantPhotosDesc = photos.filter { it.plantId == plant.id }.sortedByDescending { it.takenAt }
            val latest = plantPhotosDesc.firstOrNull()
            val currentHealth = latest?.confirmedHealth ?: latest?.aiSuggestedHealth
            PlantHealthSummary(
                plant = plant,
                latestPhoto = latest,
                currentHealth = currentHealth,
                trend = computeTrend(plantPhotosDesc)
            )
        }.sortedBy { it.currentHealth?.score ?: -1 }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun computeTrend(photosDesc: List<PlantPhotoEntity>): HealthTrend {
        val scores = photosDesc.mapNotNull { (it.confirmedHealth ?: it.aiSuggestedHealth)?.score }
        if (scores.size < 2) return HealthTrend.DESCONHECIDA
        return when {
            scores[0] > scores[1] -> HealthTrend.MELHORANDO
            scores[0] < scores[1] -> HealthTrend.PIORANDO
            else -> HealthTrend.ESTAVEL
        }
    }
}
