package com.tassiolima.regavaranda

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tassiolima.regavaranda.notifications.ReminderScheduler
import com.tassiolima.regavaranda.ui.navigation.RegaVarandaNavGraph
import com.tassiolima.regavaranda.ui.theme.RegaVarandaTheme

class MainActivity : ComponentActivity() {

    private var pendingPlantId by mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ReminderScheduler.schedule(applicationContext)
        pendingPlantId = extractPlantId(intent)

        setContent {
            RegaVarandaTheme {
                RegaVarandaNavGraph(
                    pendingPlantId = pendingPlantId,
                    onPendingPlantIdConsumed = { pendingPlantId = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPlantId = extractPlantId(intent)
    }

    private fun extractPlantId(intent: Intent?): Long? =
        intent?.getLongExtra(EXTRA_PLANT_ID, -1L)?.takeIf { it > 0 }

    companion object {
        /** Extra usado pelas notificações para abrir direto no card de uma planta específica. */
        const val EXTRA_PLANT_ID = "extra_plant_id"
    }
}
