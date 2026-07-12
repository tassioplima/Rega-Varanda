package com.tassiolima.regavaranda.domain

import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.remote.WeatherSnapshot
import com.tassiolima.regavaranda.util.DateUtils
import kotlin.math.roundToInt

data class PlantWithPlan(
    val plant: PlantEntity,
    val plan: WateringPlan,
    val status: WateringStatus,
    val sunExposure: SunExposure,
    val overwateringWarning: String? = null,
    val moistureFeedback: MoistureFeedback? = null
)

object PlantCareAdvisor {

    fun dailyTips(
        weather: WeatherSnapshot,
        sunExposure: SunExposure,
        plantsWithPlans: List<PlantWithPlan>,
        nowMillis: Long
    ): List<String> {
        val tips = mutableListOf<String>()

        val orientationLabel = sunExposure.orientation?.label ?: "não configurada"
        tips += "🌡️ Hoje: máxima de ${weather.maxTempC.roundToInt()}°C, com aproximadamente " +
            "${sunExposure.estimatedSunHours.roundToInt()}h de sol direto na sua varanda (fachada $orientationLabel)."

        val needsDoubleWatering = plantsWithPlans.filter { it.plan.timesPerDay >= 2 }
        if (needsDoubleWatering.isNotEmpty()) {
            val names = needsDoubleWatering.joinToString(", ") { it.plant.name }
            tips += "🚨 Calor e sol fortes hoje — regue $names duas vezes (manhã e fim de tarde) para evitar estresse térmico."
        }

        if (weather.precipitationProbabilityMax >= 60) {
            tips += "🌧️ Previsão de chuva de ${weather.precipitationProbabilityMax}% hoje — pode adiar a rega das plantas de exterior."
        }

        if (weather.uvIndexMax >= 8) {
            tips += "☀️ Índice UV muito alto (${weather.uvIndexMax.roundToInt()}) — regue bem cedo ou ao fim do dia, evitando o sol do meio-dia."
        }

        val todayEpochDay = DateUtils.todayEpochDay()
        val needFertilizing = plantsWithPlans.filter { pwp ->
            val plant = pwp.plant
            val lastFertilized = plant.lastFertilizedAt
            val daysSinceCreated = todayEpochDay - DateUtils.epochDayOf(plant.createdAt)
            if (lastFertilized == null) {
                daysSinceCreated >= plant.category.fertilizingDays
            } else {
                val daysSince = todayEpochDay - DateUtils.epochDayOf(lastFertilized)
                daysSince >= plant.category.fertilizingDays
            }
        }
        if (needFertilizing.isNotEmpty()) {
            val names = needFertilizing.joinToString(", ") { it.plant.name }
            tips += "💊 Já passou o tempo recomendado de adubação/vitaminas para: $names."
        }

        plantsWithPlans.randomOrNull(nowMillis)?.let { pwp ->
            tips += "💡 Dica para ${pwp.plant.name} (${pwp.plant.category.label}): ${pwp.plant.category.careTip}"
        }

        return tips
    }

    private fun <T> List<T>.randomOrNull(seed: Long): T? {
        if (isEmpty()) return null
        val index = (seed / 86_400_000L % size).toInt().let { if (it < 0) it + size else it }
        return this[index]
    }
}
