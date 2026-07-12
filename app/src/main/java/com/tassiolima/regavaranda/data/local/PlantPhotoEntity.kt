package com.tassiolima.regavaranda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tassiolima.regavaranda.data.model.HealthState

enum class AnalysisStatus { PENDING, ANALYZING, DONE, FAILED, NO_API_KEY }

@Entity(tableName = "plant_photos")
data class PlantPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val filePath: String,
    val takenAt: Long,
    val analysisStatus: AnalysisStatus = AnalysisStatus.PENDING,
    val aiSuggestedHealth: HealthState? = null,
    val confirmedHealth: HealthState? = null,
    val diagnosis: String? = null,
    val wateringTip: String? = null,
    val pruningTip: String? = null,
    val fertilizingTip: String? = null,
    val repottingTip: String? = null,
    val identifiedSpecies: String? = null,
    val recommendedWateringIntervalDays: Int? = null,
    /** Comparação com fotos anteriores desta planta (está melhorando/piorando/estável e por quê). */
    val evolutionNote: String? = null
)
