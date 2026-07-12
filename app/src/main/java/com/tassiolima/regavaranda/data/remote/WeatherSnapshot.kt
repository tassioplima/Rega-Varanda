package com.tassiolima.regavaranda.data.remote

/**
 * Previsão do dia atual resumida a partir da API pública Open-Meteo.
 * sunshineFraction é a fração do período de luz do dia com sol efetivamente
 * aberto (sunshine_duration / daylight_duration), ou seja, já descontando nuvens.
 */
data class WeatherSnapshot(
    val maxTempC: Double,
    val minTempC: Double,
    val uvIndexMax: Double,
    val precipitationProbabilityMax: Int,
    val daylightHours: Double,
    val sunshineFraction: Double
)
