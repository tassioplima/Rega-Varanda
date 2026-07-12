package com.tassiolima.regavaranda.data.repository

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tassiolima.regavaranda.data.model.Orientation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class VarandaSettings(
    val orientation: Orientation?,
    val manualSunHoursOverride: Float?,
    val lastLat: Double?,
    val lastLon: Double?
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ORIENTATION = stringPreferencesKey("orientation")
        val MANUAL_SUN_HOURS = floatPreferencesKey("manual_sun_hours")
        val LAST_LAT = doublePreferencesKey("last_lat")
        val LAST_LON = doublePreferencesKey("last_lon")
    }

    val settingsFlow: Flow<VarandaSettings> = context.dataStore.data.map { prefs ->
        VarandaSettings(
            orientation = prefs[Keys.ORIENTATION]?.let { name ->
                Orientation.entries.firstOrNull { it.name == name }
            },
            manualSunHoursOverride = prefs[Keys.MANUAL_SUN_HOURS],
            lastLat = prefs[Keys.LAST_LAT],
            lastLon = prefs[Keys.LAST_LON]
        )
    }

    suspend fun setOrientation(orientation: Orientation) {
        context.dataStore.edit { it[Keys.ORIENTATION] = orientation.name }
    }

    suspend fun setManualSunHoursOverride(hours: Float?) {
        context.dataStore.edit { prefs ->
            if (hours == null) prefs.remove(Keys.MANUAL_SUN_HOURS) else prefs[Keys.MANUAL_SUN_HOURS] = hours
        }
    }

    suspend fun setLastLocation(lat: Double, lon: Double) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_LAT] = lat
            prefs[Keys.LAST_LON] = lon
        }
    }
}
