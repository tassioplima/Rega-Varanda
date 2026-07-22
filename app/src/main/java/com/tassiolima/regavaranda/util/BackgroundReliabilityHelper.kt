package com.tassiolima.regavaranda.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Sem isenção da otimização de bateria (e, em aparelhos Xiaomi/MIUI, sem permissão de
 * "Início automático"), o Android mata o processo do app antes do WorkManager conseguir
 * rodar o lembrete de rega em segundo plano — as notificações só aparecem quando o app é
 * aberto manualmente, porque aí o trabalho atrasado finalmente consegue rodar.
 */
object BackgroundReliabilityHelper {

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        runCatching { context.startActivity(intent) }
            .onFailure { openAppSettings(context) }
    }

    fun isXiaomi(): Boolean = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    /**
     * Abre a tela de gerenciamento de "Início automático" da MIUI. O componente exato não é
     * uma API pública/documentada e pode não existir em todas as versões da MIUI/HyperOS —
     * por isso cai para a tela genérica de configurações do app se não conseguir abrir.
     */
    fun openXiaomiAutoStartSettings(context: Context) {
        val intent = Intent().apply {
            component = android.content.ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) }
            }
    }

    private fun openAppSettings(context: Context) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }
    }
}
