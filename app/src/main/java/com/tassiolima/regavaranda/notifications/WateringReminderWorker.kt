package com.tassiolima.regavaranda.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tassiolima.regavaranda.di.ServiceLocator
import com.tassiolima.regavaranda.domain.PlantPlanner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class WateringReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val settingsRepo = ServiceLocator.settingsRepository(applicationContext)
            val plantRepo = ServiceLocator.plantRepository(applicationContext)
            val weatherRepo = ServiceLocator.weatherRepository(applicationContext)

            val settings = settingsRepo.settingsFlow.first()
            val lat = settings.lastLat
            val lon = settings.lastLon
            if (lat == null || lon == null) return Result.success()

            val weather = weatherRepo.getToday(lat, lon)
            val plants = plantRepo.observePlants().first()
            val wateringLog = plantRepo.observeWateringLog().first()
            val now = System.currentTimeMillis()

            val plans = PlantPlanner.buildPlans(plants, weather, settings, wateringLog, now)
            val due = plans.filter { it.status.isDueNow }

            val doubleWateringNames = due.filter { it.plan.timesPerDay >= 2 }.map { it.plant.name }
            val normalNames = due.filter { it.plan.timesPerDay < 2 }.map { it.plant.name }

            if (due.isNotEmpty()) {
                NotificationHelper.showWateringDue(applicationContext, normalNames, doubleWateringNames)
            }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
