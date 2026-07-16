package com.tassiolima.regavaranda.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tassiolima.regavaranda.data.local.AnalysisStatus
import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.di.ServiceLocator
import kotlinx.coroutines.flow.first

class WeeklySummaryWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val plantRepo = ServiceLocator.plantRepository(applicationContext)
            val photoRepo = ServiceLocator.plantPhotoRepository(applicationContext)

            val plants = plantRepo.observePlants().first()
            if (plants.isEmpty()) return Result.success()

            val now = System.currentTimeMillis()
            val weekAgo = now - 7L * 24 * 60 * 60 * 1000

            val wateringLog = plantRepo.observeWateringLog().first().filter { it.wateredAt >= weekAgo }
            val plantsWateredThisWeek = wateringLog.map { it.plantId }.toSet()

            val neglected = plants
                .filter { plant -> plant.lastWateredAt == null || plant.lastWateredAt < weekAgo }
                .map { it.name }

            val allPhotos = photoRepo.observeAll().first()
            val concerning = plants.mapNotNull { plant ->
                val latestPhoto = allPhotos
                    .filter { it.plantId == plant.id && it.analysisStatus == AnalysisStatus.DONE }
                    .maxByOrNull { it.takenAt }
                val health = latestPhoto?.confirmedHealth ?: latestPhoto?.aiSuggestedHealth
                plant.name.takeIf { health != null && health != HealthState.SAUDAVEL }
            }

            val title = "🗓️ Resumo semanal das plantas"
            val text = buildString {
                append(
                    "${wateringLog.size} rega(s) em ${plantsWateredThisWeek.size} de " +
                        "${plants.size} planta(s) nos últimos 7 dias."
                )
                if (neglected.isNotEmpty()) {
                    append(" ⚠️ Sem rega há mais de uma semana: ${neglected.joinToString(", ")}.")
                }
                if (concerning.isNotEmpty()) {
                    append(" 🩺 Precisam de atenção: ${concerning.joinToString(", ")}.")
                }
            }

            NotificationHelper.showWeeklySummary(applicationContext, title, text)
            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
