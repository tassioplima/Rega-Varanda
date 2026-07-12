package com.tassiolima.regavaranda.domain

import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.data.model.PlantCategory
import com.tassiolima.regavaranda.data.remote.WeatherSnapshot
import com.tassiolima.regavaranda.data.repository.VarandaSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun weather(daylightHours: Double = 12.0, sunshineFraction: Double = 1.0) = WeatherSnapshot(
    maxTempC = 25.0,
    minTempC = 15.0,
    uvIndexMax = 5.0,
    precipitationProbabilityMax = 0,
    daylightHours = daylightHours,
    sunshineFraction = sunshineFraction
)

class SunExposureCalculatorTest {

    @Test
    fun `manual override takes priority over orientation sun factor`() {
        val result = SunExposureCalculator.estimate(
            weather(daylightHours = 12.0, sunshineFraction = 1.0),
            orientation = Orientation.SUL,
            manualSunHoursOverride = 5f
        )

        assertTrue(result.usedManualOverride)
        assertEquals(5.0, result.estimatedSunHours, 0.001)
    }

    @Test
    fun `orientation sun factor is used when no manual override is set`() {
        val result = SunExposureCalculator.estimate(
            weather(daylightHours = 10.0, sunshineFraction = 0.8),
            orientation = Orientation.NORTE, // sunFactor = 0.15
            manualSunHoursOverride = null
        )

        assertFalse(result.usedManualOverride)
        assertEquals(10.0 * 0.15 * 0.8, result.estimatedSunHours, 0.001)
    }

    @Test
    fun `missing orientation defaults to half sun factor`() {
        val result = SunExposureCalculator.estimate(
            weather(daylightHours = 10.0, sunshineFraction = 1.0),
            orientation = null,
            manualSunHoursOverride = null
        )

        assertEquals(10.0 * 0.5, result.estimatedSunHours, 0.001)
    }

    @Test
    fun `estimated hours never exceed the day's daylight hours`() {
        val result = SunExposureCalculator.estimate(
            weather(daylightHours = 10.0, sunshineFraction = 1.0),
            orientation = Orientation.SUL,
            manualSunHoursOverride = 20f
        )

        assertEquals(10.0, result.estimatedSunHours, 0.001)
    }

    @Test
    fun `plant-specific orientation override takes priority over varanda settings`() {
        val settings = VarandaSettings(
            orientation = Orientation.NORTE,
            manualSunHoursOverride = null,
            lastLat = null,
            lastLon = null
        )
        val plant = PlantEntity(
            name = "Planta nos fundos",
            category = PlantCategory.OUTRA,
            createdAt = System.currentTimeMillis(),
            orientationOverride = Orientation.SUL,
            sunHoursOverride = null
        )

        val result = SunExposureCalculator.estimateForPlant(weather(), settings, plant)

        assertTrue(result.isPlantSpecificLocation)
        assertEquals(Orientation.SUL, result.orientation)
    }

    @Test
    fun `plant without overrides falls back to varanda settings`() {
        val settings = VarandaSettings(
            orientation = Orientation.LESTE,
            manualSunHoursOverride = null,
            lastLat = null,
            lastLon = null
        )
        val plant = PlantEntity(
            name = "Planta da varanda",
            category = PlantCategory.OUTRA,
            createdAt = System.currentTimeMillis()
        )

        val result = SunExposureCalculator.estimateForPlant(weather(), settings, plant)

        assertFalse(result.isPlantSpecificLocation)
        assertEquals(Orientation.LESTE, result.orientation)
    }
}
