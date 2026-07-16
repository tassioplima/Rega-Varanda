package com.tassiolima.regavaranda.domain

import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.WateringLogEntity
import com.tassiolima.regavaranda.data.remote.WeatherSnapshot
import com.tassiolima.regavaranda.data.repository.VarandaSettings

/**
 * Pipeline única de "planta -> plano de rega + status + avisos", compartilhada entre a
 * Home e o worker de notificações — assim a regra nunca diverge entre a tela e o lembrete.
 */
object PlantPlanner {

    fun buildPlans(
        plants: List<PlantEntity>,
        weather: WeatherSnapshot,
        settings: VarandaSettings,
        wateringLog: List<WateringLogEntity>,
        nowMillis: Long,
        latestPhotoPathByPlant: Map<Long, String> = emptyMap()
    ): List<PlantWithPlan> {
        val logsByPlant = wateringLog.groupBy { it.plantId }
        return plants.map { plant ->
            val sunExposure = SunExposureCalculator.estimateForPlant(weather, settings, plant)
            val plan = WateringScheduler.computePlan(plant, weather, sunExposure.estimatedSunHours)
            val status = WateringScheduler.computeStatus(plan, plant, nowMillis)
            val overwateringWarning = WateringScheduler.detectOverwatering(
                plan,
                logsByPlant[plant.id].orEmpty(),
                nowMillis
            )
            val moistureFeedback = WateringScheduler.moistureFeedback(plant, nowMillis)
            PlantWithPlan(
                plant = plant,
                plan = plan,
                status = status,
                sunExposure = sunExposure,
                overwateringWarning = overwateringWarning,
                moistureFeedback = moistureFeedback,
                latestPhotoPath = latestPhotoPathByPlant[plant.id]
            )
        }
    }
}
