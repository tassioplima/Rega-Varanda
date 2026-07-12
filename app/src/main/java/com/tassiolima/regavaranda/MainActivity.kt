package com.tassiolima.regavaranda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tassiolima.regavaranda.notifications.ReminderScheduler
import com.tassiolima.regavaranda.ui.navigation.RegaVarandaNavGraph
import com.tassiolima.regavaranda.ui.theme.RegaVarandaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ReminderScheduler.schedule(applicationContext)

        setContent {
            RegaVarandaTheme {
                RegaVarandaNavGraph()
            }
        }
    }
}
