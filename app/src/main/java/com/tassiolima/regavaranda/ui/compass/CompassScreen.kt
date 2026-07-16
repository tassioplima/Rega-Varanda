package com.tassiolima.regavaranda.ui.compass

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.util.CompassReader
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    onBack: () -> Unit,
    onSelect: (Orientation) -> Unit
) {
    val context = LocalContext.current
    val compassReader = remember { CompassReader(context) }
    val hasSensor = remember { compassReader.hasCompassSensor() }
    var azimuth by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(hasSensor) {
        if (hasSensor) {
            compassReader.azimuthDegreesFlow().collect { azimuth = it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bússola") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!hasSensor) {
                Text(
                    "Este aparelho não tem sensor de bússola disponível. " +
                        "Escolha a direção manualmente na lista da tela anterior.",
                    style = MaterialTheme.typography.bodyLarge
                )
                return@Column
            }

            Text(
                "Aponte a parte de cima do celular para a direção que a sua varanda encara.",
                style = MaterialTheme.typography.bodyLarge
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(32.dp))

            val currentAzimuth = azimuth
            Box(contentAlignment = Alignment.TopCenter) {
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .graphicsLayer(rotationZ = -(currentAzimuth ?: 0f))
                        .border(BorderStroke(2.dp, MaterialTheme.colorScheme.outline), CircleShape)
                ) {
                    Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(8.dp), style = MaterialTheme.typography.titleLarge)
                    Text("L", modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp), style = MaterialTheme.typography.titleLarge)
                    Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp), style = MaterialTheme.typography.titleLarge)
                    Text("O", modifier = Modifier.align(Alignment.CenterStart).padding(8.dp), style = MaterialTheme.typography.titleLarge)
                }
                Text("▲", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(32.dp))

            val nearest = currentAzimuth?.let { Orientation.nearestTo(it) }
            Text(
                currentAzimuth?.let { "${it.roundToInt()}° — ${nearest?.label}" } ?: "Calibrando sensor...",
                style = MaterialTheme.typography.headlineSmall
            )

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(24.dp))

            Button(
                onClick = { nearest?.let(onSelect) },
                enabled = nearest != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Usar esta direção")
            }
        }
    }
}
