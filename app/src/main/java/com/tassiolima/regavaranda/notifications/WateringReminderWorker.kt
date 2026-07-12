package com.tassiolima.regavaranda.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tassiolima.regavaranda.di.ServiceLocator
import com.tassiolima.regavaranda.domain.SunExposureCalculator
import com.tassiolima.regavaranda.domain.WateringScheduler
import kotlinx.coroutines.flow.first

class WateringReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settingsRepo = ServiceLocator.settingsRepository(applicationContext)
            val plantRepo = ServiceLocator.plantRepository(applicationContext)
            val weatherClient = ServiceLocator.weatherClient()

            val settings = settingsRepo.settingsFlow.first()
            val lat = settings.lastLat
            val lon = settings.lastLon
            if (lat == null || lon == null) return Result.success()

            val weather = weatherClient.fetchToday(lat, lon)

            val plants = plantRepo.observePlants().first()
            val now = System.currentTimeMillis()

            val plansByPlant = plants.associateWith { plant ->
                val plantSunExposure = SunExposureCalculator.estimateForPlant(weather, settings, plant)
                WateringScheduler.computePlan(
                    plant,
                    weather,
                    plantSunExposure.estimatedSunHours
                )
            }

            val duePlants = plants.filter { plant ->
                WateringScheduler.computeStatus(plansByPlant[plant]!!, plant, now).isDueNow
            }

            val doubleWateringNames = duePlants.filter { plansByPlant[it]!!.timesPerDay >= 2 }.map { it.name }
            val normalNames = duePlants.filterNot { plansByPlant[it]!!.timesPerDay >= 2 }.map { it.name }

            if (duePlants.isNotEmpty()) {
                NotificationHelper.showWateringDue(applicationContext, normalNames, doubleWateringNames)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
