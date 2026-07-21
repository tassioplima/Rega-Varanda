package com.tassiolima.regavaranda.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tassiolima.regavaranda.MainActivity
import com.tassiolima.regavaranda.R

object NotificationHelper {
    private const val CHANNEL_ID = "watering_reminders"
    private const val NOTIFICATION_ID = 1001
    private const val WEEKLY_CHANNEL_ID = "weekly_summary"
    private const val WEEKLY_NOTIFICATION_ID = 1002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembretes de rega",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Avisos de quando regar suas plantas da varanda"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /** Toque na notificação abre a planta em questão (se só uma estiver pendente) ou a Home. */
    private fun contentIntent(context: Context, plantId: Long?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (plantId != null) putExtra(MainActivity.EXTRA_PLANT_ID, plantId)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun showWateringDue(context: Context, normalPlants: List<Pair<Long, String>>, doubleWateringPlants: List<Pair<Long, String>>) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

        val allDue = normalPlants + doubleWateringPlants
        val normalNames = normalPlants.map { it.second }
        val doubleWateringNames = doubleWateringPlants.map { it.second }

        val title: String
        val text: String
        if (doubleWateringNames.isNotEmpty()) {
            title = "🔥 Sol forte hoje — regue de novo!"
            text = buildString {
                append("Com calor e sol forte, regue mais uma vez hoje: ${doubleWateringNames.joinToString(", ")}.")
                if (normalNames.isNotEmpty()) {
                    append(" Também precisam de rega: ${normalNames.joinToString(", ")}.")
                }
            }
        } else {
            title = if (normalNames.size == 1) "Hora de regar!" else "Hora de regar ${normalNames.size} plantas!"
            text = "Precisam de água agora: ${normalNames.joinToString(", ")}"
        }

        // Com uma única planta pendente, leva direto pra ela; com várias, abre a Home.
        val targetPlantId = allDue.singleOrNull()?.first

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, targetPlantId, NOTIFICATION_ID))
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun ensureWeeklyChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WEEKLY_CHANNEL_ID,
                "Resumo semanal",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Resumo semanal de regas e saúde das plantas da varanda"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showWeeklySummary(context: Context, title: String, text: String) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureWeeklyChannel(context)

        val notification = NotificationCompat.Builder(context, WEEKLY_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context, plantId = null, requestCode = WEEKLY_NOTIFICATION_ID))
            .build()

        NotificationManagerCompat.from(context).notify(WEEKLY_NOTIFICATION_ID, notification)
    }
}
