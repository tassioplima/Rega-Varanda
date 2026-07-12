package com.tassiolima.regavaranda.domain

import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.data.remote.WeatherSnapshot
import com.tassiolima.regavaranda.data.repository.VarandaSettings

data class SunExposure(
    val estimatedSunHours: Double,
    val orientation: Orientation?,
    val usedManualOverride: Boolean,
    val isPlantSpecificLocation: Boolean = false
)

object SunExposureCalculator {

    /**
     * Estima quantas horas de sol direto a varanda recebe hoje, combinando:
     * - a orientação da fachada (quanto da luz do dia aquela direção recebe), ou
     *   uma estimativa manual do usuário, que tem prioridade por vir de observação real;
     * - a fração do dia que realmente terá céu aberto (sunshineFraction já descontando nuvens).
     */
    fun estimate(
        weather: WeatherSnapshot,
        orientation: Orientation?,
        manualSunHoursOverride: Float?,
        isPlantSpecificLocation: Boolean = false
    ): SunExposure {
        val baseHours = if (manualSunHoursOverride != null) {
            manualSunHoursOverride.toDouble()
        } else {
            weather.daylightHours * (orientation?.sunFactor ?: 0.5)
        }

        val estimated = (baseHours * weather.sunshineFraction).coerceIn(0.0, weather.daylightHours)

        return SunExposure(
            estimatedSunHours = estimated,
            orientation = orientation,
            usedManualOverride = manualSunHoursOverride != null,
            isPlantSpecificLocation = isPlantSpecificLocation
        )
    }

    /**
     * Igual a [estimate], mas usa a orientação/estimativa específica da planta quando ela
     * estiver configurada em um local diferente da varanda principal (ex.: nos fundos da casa).
     */
    fun estimateForPlant(weather: WeatherSnapshot, settings: VarandaSettings, plant: PlantEntity): SunExposure {
        val hasOverride = plant.orientationOverride != null || plant.sunHoursOverride != null
        val orientation = plant.orientationOverride ?: settings.orientation
        val manualHours = plant.sunHoursOverride ?: settings.manualSunHoursOverride
        return estimate(weather, orientation, manualHours, isPlantSpecificLocation = hasOverride)
    }
}
