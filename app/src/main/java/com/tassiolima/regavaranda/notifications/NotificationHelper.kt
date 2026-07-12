package com.tassiolima.regavaranda.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tassiolima.regavaranda.R

object NotificationHelper {
    private const val CHANNEL_ID = "watering_reminders"
    private const val NOTIFICATION_ID = 1001

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

    fun showWateringDue(context: Context, normalNames: List<String>, doubleWateringNames: List<String>) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        ensureChannel(context)

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

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
