package com.tassiolima.regavaranda.domain

import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.WateringLogEntity
import com.tassiolima.regavaranda.data.model.PlantCategory
import com.tassiolima.regavaranda.data.remote.WeatherSnapshot
import com.tassiolima.regavaranda.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val DAY_MILLIS = 86_400_000L

private fun testPlant(
    category: PlantCategory = PlantCategory.OUTRA,
    customIntervalDays: Int? = null,
    aiWateringIntervalDays: Int? = null,
    lastWateredAt: Long? = null,
    waterCountToday: Int = 0,
    waterCountDayEpoch: Long = 0,
    soilMoistureLevel: Int? = null,
    soilMoistureReadingAt: Long? = null
) = PlantEntity(
    name = "Planta teste",
    category = category,
    customIntervalDays = customIntervalDays,
    createdAt = System.currentTimeMillis(),
    lastWateredAt = lastWateredAt,
    waterCountToday = waterCountToday,
    waterCountDayEpoch = waterCountDayEpoch,
    aiWateringIntervalDays = aiWateringIntervalDays,
    soilMoistureLevel = soilMoistureLevel,
    soilMoistureReadingAt = soilMoistureReadingAt
)

private fun weather(
    maxTempC: Double = 22.0,
    precipitationProbabilityMax: Int = 0
) = WeatherSnapshot(
    maxTempC = maxTempC,
    minTempC = maxTempC - 8,
    uvIndexMax = 5.0,
    precipitationProbabilityMax = precipitationProbabilityMax,
    daylightHours = 12.0,
    sunshineFraction = 0.8
)

class WateringSchedulerTest {

    @Test
    fun `hot and sunny day triggers multiple waterings per day`() {
        val plant = testPlant(category = PlantCategory.HORTALICA_LEGUME) // baseWateringDays = 1
        val plan = WateringScheduler.computePlan(plant, weather(maxTempC = 33.0), estimatedSunHours = 7.0)

        assertEquals(1, plan.intervalDays)
        assertTrue(plan.timesPerDay >= 2)
    }

    @Test
    fun `high rain chance stretches the interval`() {
        val plant = testPlant(category = PlantCategory.FLOR_ORNAMENTAL) // baseWateringDays = 3
        val plan = WateringScheduler.computePlan(
            plant,
            weather(maxTempC = 20.0, precipitationProbabilityMax = 70),
            estimatedSunHours = 2.0
        )

        assertEquals(1, plan.timesPerDay)
        assertEquals(5, plan.intervalDays) // 3 * 1.6 = 4.8 -> rounds to 5
        assertTrue(plan.reason.contains("chuva"))
    }

    @Test
    fun `manual interval always wins over AI and category defaults`() {
        val plant = testPlant(
            category = PlantCategory.FOLHAGEM_TROPICAL, // baseWateringDays = 4
            customIntervalDays = 7,
            aiWateringIntervalDays = 2
        )
        val plan = WateringScheduler.computePlan(plant, weather(), estimatedSunHours = 2.0)

        assertEquals(7, plan.intervalDays)
        assertEquals(BaseIntervalSource.MANUAL, plan.baseIntervalSource)
    }

    @Test
    fun `AI identified interval wins over category default when no manual override`() {
        val plant = testPlant(category = PlantCategory.FOLHAGEM_TROPICAL, aiWateringIntervalDays = 5)
        val plan = WateringScheduler.computePlan(plant, weather(), estimatedSunHours = 2.0)

        assertEquals(5, plan.intervalDays)
        assertEquals(BaseIntervalSource.AI_IDENTIFIED, plan.baseIntervalSource)
        assertTrue(plan.reason.contains("espécie identificada pela IA"))
    }

    @Test
    fun `plant never watered is always due now`() {
        val plant = testPlant(lastWateredAt = null)
        val plan = WateringScheduler.computePlan(plant, weather(), estimatedSunHours = 2.0)
        val status = WateringScheduler.computeStatus(plan, plant, System.currentTimeMillis())

        assertTrue(status.isDueNow)
        assertEquals(plan.timesPerDay, status.timesRemainingToday)
    }

    @Test
    fun `multiple times per day plan tracks remaining count today`() {
        val today = DateUtils.todayEpochDay()
        val now = System.currentTimeMillis()
        val basePlant = testPlant(category = PlantCategory.HORTALICA_LEGUME)
        val plan = WateringScheduler.computePlan(basePlant, weather(maxTempC = 33.0), estimatedSunHours = 7.0)
        assertTrue(plan.timesPerDay >= 2)

        val plant = testPlant(
            category = PlantCategory.HORTALICA_LEGUME,
            lastWateredAt = now,
            waterCountToday = plan.timesPerDay - 1,
            waterCountDayEpoch = today
        )
        val status = WateringScheduler.computeStatus(plan, plant, now)
        assertTrue(status.isDueNow)
        assertEquals(1, status.timesRemainingToday)
    }

    @Test
    fun `multiple times per day plan is not due once daily count is reached`() {
        val today = DateUtils.todayEpochDay()
        val now = System.currentTimeMillis()
        val basePlant = testPlant(category = PlantCategory.HORTALICA_LEGUME)
        val plan = WateringScheduler.computePlan(basePlant, weather(maxTempC = 33.0), estimatedSunHours = 7.0)

        val plant = testPlant(
            category = PlantCategory.HORTALICA_LEGUME,
            lastWateredAt = now,
            waterCountToday = plan.timesPerDay,
            waterCountDayEpoch = today
        )
        val status = WateringScheduler.computeStatus(plan, plant, now)
        assertFalse(status.isDueNow)
        assertEquals(0, status.timesRemainingToday)
    }

    @Test
    fun `single watering per day plan is not due before the interval elapses`() {
        val now = System.currentTimeMillis()
        val plant = testPlant(customIntervalDays = 3, lastWateredAt = now - DAY_MILLIS)
        val plan = WateringScheduler.computePlan(plant, weather(), estimatedSunHours = 2.0)

        val status = WateringScheduler.computeStatus(plan, plant, now)
        assertFalse(status.isDueNow)
        assertNotNull(status.nextDueAtMillis)
    }

    @Test
    fun `single watering per day plan is due once the interval elapses`() {
        val now = System.currentTimeMillis()
        val plant = testPlant(customIntervalDays = 3, lastWateredAt = now - 4 * DAY_MILLIS)
        val plan = WateringScheduler.computePlan(plant, weather(), estimatedSunHours = 2.0)

        val status = WateringScheduler.computeStatus(plan, plant, now)
        assertTrue(status.isDueNow)
        assertNull(status.nextDueAtMillis)
    }

    @Test
    fun `formatCountdown formats minutes, hours and days`() {
        assertEquals("agora", WateringScheduler.formatCountdown(0))
        assertEquals("agora", WateringScheduler.formatCountdown(-1000))
        assertEquals("45min", WateringScheduler.formatCountdown(45 * 60_000L))
        assertEquals("3h", WateringScheduler.formatCountdown(3 * 60 * 60_000L))
        assertEquals("2d", WateringScheduler.formatCountdown(2 * DAY_MILLIS))
        assertEquals("2d 5h", WateringScheduler.formatCountdown(2 * DAY_MILLIS + 5 * 60 * 60_000L))
    }

    @Test
    fun `overwatering warning triggers when watered far more often than recommended`() {
        val plan = WateringPlan(timesPerDay = 1, intervalDays = 7, reason = "", baseIntervalSource = BaseIntervalSource.CATEGORY_DEFAULT)
        val now = System.currentTimeMillis()
        val log = (0 until 5).map { WateringLogEntity(plantId = 1, wateredAt = now - it * DAY_MILLIS) }

        val warning = WateringScheduler.detectOverwatering(plan, log, now)
        assertNotNull(warning)
        assertTrue(warning!!.contains("5 vezes"))
    }

    @Test
    fun `overwatering warning is null when frequency is within expectations`() {
        val plan = WateringPlan(timesPerDay = 1, intervalDays = 2, reason = "", baseIntervalSource = BaseIntervalSource.CATEGORY_DEFAULT)
        val now = System.currentTimeMillis()
        val log = (0 until 4).map { WateringLogEntity(plantId = 1, wateredAt = now - it * DAY_MILLIS) }

        assertNull(WateringScheduler.detectOverwatering(plan, log, now))
    }

    @Test
    fun `overwatering is never flagged for multiple-times-per-day plans`() {
        val plan = WateringPlan(timesPerDay = 2, intervalDays = 1, reason = "", baseIntervalSource = BaseIntervalSource.CATEGORY_DEFAULT)
        val now = System.currentTimeMillis()
        val log = (0 until 10).map { WateringLogEntity(plantId = 1, wateredAt = now - it * DAY_MILLIS) }

        assertNull(WateringScheduler.detectOverwatering(plan, log, now))
    }

    @Test
    fun `moisture feedback is null without a reading`() {
        val plant = testPlant(soilMoistureLevel = null)
        assertNull(WateringScheduler.moistureFeedback(plant, System.currentTimeMillis()))
    }

    @Test
    fun `moisture feedback flags dry soil as needing water`() {
        val now = System.currentTimeMillis()
        // OUTRA has dryMoistureThreshold = 4
        val plant = testPlant(soilMoistureLevel = 3, soilMoistureReadingAt = now)
        val feedback = WateringScheduler.moistureFeedback(plant, now)

        assertNotNull(feedback)
        assertTrue(feedback!!.suggestsWatering)
        assertTrue(feedback.message.contains("seco"))
        assertTrue(feedback.message.contains("medido hoje"))
    }

    @Test
    fun `moisture feedback reports wet soil as not needing water`() {
        val now = System.currentTimeMillis()
        val plant = testPlant(soilMoistureLevel = 9, soilMoistureReadingAt = now - DAY_MILLIS)
        val feedback = WateringScheduler.moistureFeedback(plant, now)

        assertNotNull(feedback)
        assertFalse(feedback!!.suggestsWatering)
        assertTrue(feedback.message.contains("úmido"))
        assertTrue(feedback.message.contains("medido ontem"))
    }

    @Test
    fun `moisture feedback ages the reading label in days`() {
        val now = System.currentTimeMillis()
        val plant = testPlant(soilMoistureLevel = 5, soilMoistureReadingAt = now - 5 * DAY_MILLIS)
        val feedback = WateringScheduler.moistureFeedback(plant, now)

        assertNotNull(feedback)
        assertTrue(feedback!!.message.contains("há 5 dias"))
    }
}
