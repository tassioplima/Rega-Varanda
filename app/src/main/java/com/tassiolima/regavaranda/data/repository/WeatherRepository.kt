package com.tassiolima.regavaranda.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.tassiolima.regavaranda.data.remote.OpenMeteoClient
import com.tassiolima.regavaranda.data.remote.WeatherSnapshot
import com.tassiolima.regavaranda.util.DateUtils
import org.json.JSONObject
import kotlin.math.abs

/**
 * Camada de cache sobre a Open-Meteo: guarda a última previsão do dia por localização,
 * para que abrir o app (ou rodar o worker) não refaça GPS + rede desnecessariamente e a
 * Home carregue instantânea mesmo offline, desde que já tenha buscado o clima de hoje.
 */
class WeatherRepository(
    context: Context,
    private val client: OpenMeteoClient
) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    suspend fun getToday(latitude: Double, longitude: Double, forceRefresh: Boolean = false): WeatherSnapshot {
        if (!forceRefresh) {
            readCache(latitude, longitude)?.let { return it }
        }
        return client.fetchToday(latitude, longitude).also { writeCache(latitude, longitude, it) }
    }

    private fun readCache(latitude: Double, longitude: Double): WeatherSnapshot? {
        val json = prefs.getString(KEY_SNAPSHOT, null) ?: return null
        val cachedEpochDay = prefs.getLong(KEY_EPOCH_DAY, -1)
        val cachedAt = prefs.getLong(KEY_FETCHED_AT, 0)
        val cachedLat = prefs.getFloat(KEY_LAT, Float.NaN)
        val cachedLon = prefs.getFloat(KEY_LON, Float.NaN)

        val sameDay = cachedEpochDay == DateUtils.todayEpochDay()
        val fresh = System.currentTimeMillis() - cachedAt < MAX_AGE_MILLIS
        val sameArea = !cachedLat.isNaN() && !cachedLon.isNaN() &&
            abs(cachedLat - latitude) < LOCATION_TOLERANCE_DEG &&
            abs(cachedLon - longitude) < LOCATION_TOLERANCE_DEG

        if (!sameDay || !fresh || !sameArea) return null
        return runCatching { fromJson(JSONObject(json)) }.getOrNull()
    }

    private fun writeCache(latitude: Double, longitude: Double, snapshot: WeatherSnapshot) {
        prefs.edit()
            .putString(KEY_SNAPSHOT, toJson(snapshot).toString())
            .putLong(KEY_EPOCH_DAY, DateUtils.todayEpochDay())
            .putLong(KEY_FETCHED_AT, System.currentTimeMillis())
            .putFloat(KEY_LAT, latitude.toFloat())
            .putFloat(KEY_LON, longitude.toFloat())
            .apply()
    }

    private fun toJson(s: WeatherSnapshot) = JSONObject().apply {
        put("maxTempC", s.maxTempC)
        put("minTempC", s.minTempC)
        put("uvIndexMax", s.uvIndexMax)
        put("precipitationProbabilityMax", s.precipitationProbabilityMax)
        put("daylightHours", s.daylightHours)
        put("sunshineFraction", s.sunshineFraction)
    }

    private fun fromJson(json: JSONObject) = WeatherSnapshot(
        maxTempC = json.getDouble("maxTempC"),
        minTempC = json.getDouble("minTempC"),
        uvIndexMax = json.getDouble("uvIndexMax"),
        precipitationProbabilityMax = json.getInt("precipitationProbabilityMax"),
        daylightHours = json.getDouble("daylightHours"),
        sunshineFraction = json.getDouble("sunshineFraction")
    )

    companion object {
        private const val KEY_SNAPSHOT = "snapshot_json"
        private const val KEY_EPOCH_DAY = "epoch_day"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val KEY_LAT = "lat"
        private const val KEY_LON = "lon"
        private const val MAX_AGE_MILLIS = 3 * 60 * 60 * 1000L // 3 horas
        private const val LOCATION_TOLERANCE_DEG = 0.05f // ~5 km
    }
}
