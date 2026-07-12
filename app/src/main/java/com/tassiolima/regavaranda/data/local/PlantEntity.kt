package com.tassiolima.regavaranda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.data.model.PlantCategory

@Entity(tableName = "plants")
data class PlantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: PlantCategory,
    val customIntervalDays: Int? = null,
    val notes: String = "",
    val createdAt: Long,
    val lastWateredAt: Long? = null,
    val waterCountToday: Int = 0,
    val waterCountDayEpoch: Long = 0,
    val lastFertilizedAt: Long? = null,
    /** Se não nulo, esta planta está em um local com orientação diferente da varanda principal. */
    val orientationOverride: Orientation? = null,
    val sunHoursOverride: Float? = null,
    /** Espécie identificada pela IA na análise de foto mais recente. */
    val identifiedSpecies: String? = null,
    /** Intervalo de rega em dias recomendado pela IA para a espécie identificada. */
    val aiWateringIntervalDays: Int? = null,
    /** Última leitura do medidor de umidade do solo (escala 1-10, 1=seco/10=molhado). */
    val soilMoistureLevel: Int? = null,
    val soilMoistureReadingAt: Long? = null
)
