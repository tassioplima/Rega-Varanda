package com.tassiolima.regavaranda.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

object ReminderScheduler {

    /**
     * Regar de manhã é melhor do que à tarde/sol forte do meio-dia: a água tem tempo de ser
     * absorvida antes do calor, e evita queimar folhas com gotas sob sol direto. Por isso a
     * checagem principal (que avisa qualquer planta que precise de rega) roda de manhã; a
     * checagem da noite só reforça plantas com rega 2x/dia que ainda faltam regar de novo hoje
     * — não repete o aviso de plantas de 1x/dia que já deveriam ter sido regadas de manhã.
     */
    fun schedule(context: Context) {
        NotificationHelper.ensureChannel(context)
        // Substituído pela checagem "watering_check_evening" abaixo — cancela para quem
        // instalou uma versão anterior e ainda tem esse worker antigo agendado (KEEP não
        // o tocaria sozinho, então ficaria rodando para sempre em paralelo ao novo).
        WorkManager.getInstance(context).cancelUniqueWork("watering_check_afternoon")
        scheduleDailyCheck(context, uniqueName = "watering_check_morning", hour = 7, onlyDoubleWatering = false)
        scheduleDailyCheck(context, uniqueName = "watering_check_evening", hour = 18, onlyDoubleWatering = true)

        NotificationHelper.ensureWeeklyChannel(context)
        scheduleWeeklySummary(context)
    }

    private fun scheduleWeeklySummary(context: Context) {
        val initialDelay = delayUntilNextWeekday(DayOfWeek.SUNDAY, hour = 18)
        val request = PeriodicWorkRequestBuilder<WeeklySummaryWorker>(Duration.ofDays(7))
            .setInitialDelay(initialDelay)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "weekly_summary",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun delayUntilNextWeekday(dayOfWeek: DayOfWeek, hour: Int): Duration {
        val now = ZonedDateTime.now()
        var target = ZonedDateTime.of(LocalDate.now(), LocalTime.of(hour, 0), now.zone)
            .with(TemporalAdjusters.nextOrSame(dayOfWeek))
        if (!target.isAfter(now)) {
            target = target.plusWeeks(1)
        }
        return Duration.between(now, target)
    }

    private fun scheduleDailyCheck(context: Context, uniqueName: String, hour: Int, onlyDoubleWatering: Boolean) {
        val initialDelay = delayUntilNext(hour)
        val request = PeriodicWorkRequestBuilder<WateringReminderWorker>(Duration.ofHours(24))
            .setInitialDelay(initialDelay)
            .setInputData(Data.Builder().putBoolean(WateringReminderWorker.KEY_ONLY_DOUBLE_WATERING, onlyDoubleWatering).build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            uniqueName,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun delayUntilNext(hour: Int): Duration {
        val now = ZonedDateTime.now()
        var target = ZonedDateTime.of(LocalDate.now(), LocalTime.of(hour, 0), now.zone)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }
        return Duration.between(now, target)
    }
}
