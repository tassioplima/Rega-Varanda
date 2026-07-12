package com.tassiolima.regavaranda.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class OpenMeteoClient(private val client: OkHttpClient = OkHttpClient()) {

    suspend fun fetchToday(latitude: Double, longitude: Double): WeatherSnapshot =
        withContext(Dispatchers.IO) {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&daily=temperature_2m_max,temperature_2m_min,uv_index_max," +
                "precipitation_probability_max,daylight_duration,sunshine_duration" +
                "&timezone=auto&forecast_days=1"

            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Open-Meteo respondeu ${response.code}")
                }
                val body = response.body?.string() ?: throw IOException("Resposta vazia da Open-Meteo")
                parse(body)
            }
        }

    private fun parse(body: String): WeatherSnapshot {
        val daily = JSONObject(body).getJSONObject("daily")
        val maxTemp = daily.getJSONArray("temperature_2m_max").getDouble(0)
        val minTemp = daily.getJSONArray("temperature_2m_min").getDouble(0)
        val uvMax = daily.optJSONArray("uv_index_max")?.optDouble(0, 0.0) ?: 0.0
        val precipProb = daily.optJSONArray("precipitation_probability_max")?.optInt(0, 0) ?: 0
        val daylightSeconds = daily.getJSONArray("daylight_duration").getDouble(0)
        val sunshineSeconds = daily.optJSONArray("sunshine_duration")?.optDouble(0, daylightSeconds * 0.7)
            ?: (daylightSeconds * 0.7)

        val daylightHours = daylightSeconds / 3600.0
        val sunshineFraction = if (daylightSeconds > 0) {
            (sunshineSeconds / daylightSeconds).coerceIn(0.0, 1.0)
        } else 0.0

        return WeatherSnapshot(
            maxTempC = maxTemp,
            minTempC = minTemp,
            uvIndexMax = uvMax,
            precipitationProbabilityMax = precipProb,
            daylightHours = daylightHours,
            sunshineFraction = sunshineFraction
        )
    }
}
