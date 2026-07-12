package com.tassiolima.regavaranda.notifications

import android.content.Context
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

    fun schedule(context: Context) {
        NotificationHelper.ensureChannel(context)
        scheduleDailyCheck(context, uniqueName = "watering_check_morning", hour = 8)
        scheduleDailyCheck(context, uniqueName = "watering_check_afternoon", hour = 17)

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

    private fun scheduleDailyCheck(context: Context, uniqueName: String, hour: Int) {
        val initialDelay = delayUntilNext(hour)
        val request = PeriodicWorkRequestBuilder<WateringReminderWorker>(Duration.ofHours(24))
            .setInitialDelay(initialDelay)
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
