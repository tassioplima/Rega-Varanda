package com.tassiolima.regavaranda.domain

import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.data.model.PlantCategory
import com.tassiolima.regavaranda.data.remote.WeatherSnapshot
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY_MILLIS = 86_400_000L

private fun weather(
    maxTempC: Double = 22.0,
    precipitationProbabilityMax: Int = 0,
    uvIndexMax: Double = 5.0
) = WeatherSnapshot(
    maxTempC = maxTempC,
    minTempC = maxTempC - 8,
    uvIndexMax = uvIndexMax,
    precipitationProbabilityMax = precipitationProbabilityMax,
    daylightHours = 12.0,
    sunshineFraction = 0.8
)

private fun sunExposure() = SunExposure(estimatedSunHours = 6.0, orientation = Orientation.SUL, usedManualOverride = false)

private fun plantWithPlan(
    name: String = "Planta teste",
    timesPerDay: Int = 1,
    createdAt: Long = System.currentTimeMillis(),
    lastFertilizedAt: Long? = null,
    category: PlantCategory = PlantCategory.OUTRA
): PlantWithPlan {
    val plant = PlantEntity(name = name, category = category, createdAt = createdAt, lastFertilizedAt = lastFertilizedAt)
    val plan = WateringPlan(timesPerDay = timesPerDay, intervalDays = 1, reason = "", baseIntervalSource = BaseIntervalSource.CATEGORY_DEFAULT)
    val status = WateringStatus(isDueNow = false, timesRemainingToday = 0, daysUntilNext = 1)
    return PlantWithPlan(plant = plant, plan = plan, status = status, sunExposure = sunExposure())
}

class PlantCareAdvisorTest {

    @Test
    fun `daily tips always open with a temperature and sun summary`() {
        val tips = PlantCareAdvisor.dailyTips(weather(), sunExposure(), listOf(plantWithPlan()), nowMillis = 0L)

        assertTrue(tips.first().contains("Hoje"))
        assertTrue(tips.first().contains("°C"))
    }

    @Test
    fun `double watering plants trigger a dedicated warning`() {
        val plants = listOf(plantWithPlan(name = "Suculenta", timesPerDay = 2))
        val tips = PlantCareAdvisor.dailyTips(weather(maxTempC = 35.0), sunExposure(), plants, nowMillis = 0L)

        assertTrue(tips.any { it.contains("duas vezes") && it.contains("Suculenta") })
    }

    @Test
    fun `high rain chance adds a rain tip`() {
        val tips = PlantCareAdvisor.dailyTips(
            weather(precipitationProbabilityMax = 75),
            sunExposure(),
            listOf(plantWithPlan()),
            nowMillis = 0L
        )

        assertTrue(tips.any { it.contains("chuva") && it.contains("75%") })
    }

    @Test
    fun `no rain tip when precipitation chance is low`() {
        val tips = PlantCareAdvisor.dailyTips(
            weather(precipitationProbabilityMax = 10),
            sunExposure(),
            listOf(plantWithPlan()),
            nowMillis = 0L
        )

        assertTrue(tips.none { it.contains("chuva") })
    }

    @Test
    fun `very high UV index adds a sun protection tip`() {
        val tips = PlantCareAdvisor.dailyTips(
            weather(uvIndexMax = 9.0),
            sunExposure(),
            listOf(plantWithPlan()),
            nowMillis = 0L
        )

        assertTrue(tips.any { it.contains("UV") })
    }

    @Test
    fun `plants overdue for fertilizing are called out by name`() {
        val overdue = plantWithPlan(
            name = "Samambaia",
            createdAt = System.currentTimeMillis() - 30 * DAY_MILLIS, // OUTRA.fertilizingDays = 21
            category = PlantCategory.OUTRA
        )
        val tips = PlantCareAdvisor.dailyTips(weather(), sunExposure(), listOf(overdue), nowMillis = 0L)

        assertTrue(tips.any { it.contains("adubação") && it.contains("Samambaia") })
    }

    @Test
    fun `recently fertilized plants are not flagged again`() {
        val recentlyFertilized = plantWithPlan(
            name = "Samambaia",
            createdAt = System.currentTimeMillis() - 30 * DAY_MILLIS,
            lastFertilizedAt = System.currentTimeMillis() - DAY_MILLIS,
            category = PlantCategory.OUTRA
        )
        val tips = PlantCareAdvisor.dailyTips(weather(), sunExposure(), listOf(recentlyFertilized), nowMillis = 0L)

        assertTrue(tips.none { it.contains("adubação") })
    }

    @Test
    fun `care tip highlights one of the plants`() {
        val tips = PlantCareAdvisor.dailyTips(weather(), sunExposure(), listOf(plantWithPlan(name = "Zamioculca")), nowMillis = 0L)

        assertTrue(tips.any { it.contains("Dica para Zamioculca") })
    }

    @Test
    fun `empty plant list still returns the weather summary without crashing`() {
        val tips = PlantCareAdvisor.dailyTips(weather(), sunExposure(), emptyList(), nowMillis = 0L)

        assertTrue(tips.isNotEmpty())
        assertTrue(tips.none { it.contains("Dica para") })
    }
}
