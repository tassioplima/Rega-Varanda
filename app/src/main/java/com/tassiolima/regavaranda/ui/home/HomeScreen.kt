package com.tassiolima.regavaranda.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.domain.PlantWithPlan
import com.tassiolima.regavaranda.domain.WateringScheduler
import com.tassiolima.regavaranda.util.DateUtils
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddPlant: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDashboard: () -> Unit,
    onOpenPlant: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            viewModel.refreshLocationAndWeather()
        }
    }
    val onRequestLocationPermission: () -> Unit = {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions += Manifest.permission.POST_NOTIFICATIONS
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rega Varanda") },
                actions = {
                    IconButton(onClick = onOpenDashboard) {
                        Icon(Icons.Filled.Favorite, contentDescription = "Saúde das plantas")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Configurações da varanda")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlant) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar planta")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                !state.hasLocationPermission -> PermissionRequestContent(onRequestLocationPermission)
                state.isLoading && state.weather == null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                else -> HomeContent(state, viewModel, onOpenPlant)
            }
        }
    }
}

@Composable
private fun PermissionRequestContent(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Para calcular o sol que a sua varanda recebe, o Rega Varanda precisa da sua localização.",
            style = MaterialTheme.typography.bodyLarge
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(16.dp))
        Button(onClick = onRequestPermission) { Text("Permitir localização") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(state: HomeUiState, viewModel: HomeViewModel, onOpenPlant: (Long) -> Unit) {
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.refreshLocationAndWeather() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.errorMessage?.let { error ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(error, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (state.weather != null && state.sunExposure != null) {
                item { WeatherCard(state) }
            }

            if (state.dailyTips.isNotEmpty()) {
                item { TipsCard(state.dailyTips) }
            }

            if (state.plantsWithPlans.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Você ainda não adicionou nenhuma planta. Toque no + para começar.",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            items(state.plantsWithPlans) { pwp ->
                PlantCard(
                    pwp = pwp,
                    onWater = { viewModel.markWatered(pwp.plant) },
                    onUndoWater = { viewModel.undoWatering(pwp.plant) },
                    onDelete = { viewModel.deletePlant(pwp.plant) },
                    onOpenDetail = { onOpenPlant(pwp.plant.id) }
                )
            }
        }
    }
}

@Composable
private fun WeatherCard(state: HomeUiState) {
    val weather = state.weather!!
    val sun = state.sunExposure!!
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Clima e sol na varanda", style = MaterialTheme.typography.titleMedium)
            Text("🌡️ Máxima de ${weather.maxTempC.roundToInt()}°C / mínima de ${weather.minTempC.roundToInt()}°C")
            Text("☀️ Sol direto estimado hoje: ~${sun.estimatedSunHours.roundToInt()}h")
            Text("🧭 Fachada configurada: ${state.orientation?.label ?: "não configurada"}")
            Text("🔆 Índice UV máximo: ${weather.uvIndexMax.roundToInt()}")
            if (weather.precipitationProbabilityMax > 0) {
                Text("🌧️ Chance de chuva: ${weather.precipitationProbabilityMax}%")
            }
        }
    }
}

@Composable
private fun TipsCard(tips: List<String>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Dicas de hoje", style = MaterialTheme.typography.titleMedium)
            tips.forEach { tip -> Text(tip, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun PlantCard(
    pwp: PlantWithPlan,
    onWater: () -> Unit,
    onUndoWater: () -> Unit,
    onDelete: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val plant: PlantEntity = pwp.plant
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${plant.category.emoji} ${plant.name}",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remover planta")
                }
            }
            Text(plant.category.label, style = MaterialTheme.typography.bodySmall)
            plant.identifiedSpecies?.takeIf { it.isNotBlank() }?.let { species ->
                Text("🔬 Identificado pela IA: $species", style = MaterialTheme.typography.bodySmall)
            }
            if (pwp.sunExposure.isPlantSpecificLocation) {
                Text(
                    "📍 Local próprio: ${pwp.sunExposure.orientation?.label ?: "sol manual"} " +
                        "(~${pwp.sunExposure.estimatedSunHours.roundToInt()}h de sol)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(pwp.plan.reason, style = MaterialTheme.typography.bodySmall)

            val statusText = when {
                pwp.status.isDueNow && pwp.plan.timesPerDay >= 2 ->
                    "🚨 Regar agora (${pwp.status.timesRemainingToday}x restante(s) hoje)"
                pwp.status.isDueNow -> "💧 Regar agora"
                pwp.status.nextDueAtMillis != null -> {
                    val remaining = pwp.status.nextDueAtMillis - System.currentTimeMillis()
                    "⏳ Próxima rega em ${WateringScheduler.formatCountdown(remaining)}"
                }
                else -> "✅ Regada hoje"
            }
            Text(
                statusText,
                style = MaterialTheme.typography.titleSmall,
                color = if (pwp.status.isDueNow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )

            pwp.overwateringWarning?.let { warning ->
                Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            pwp.moistureFeedback?.let { feedback ->
                Text(
                    feedback.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (feedback.suggestsWatering) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            if (plant.notes.isNotBlank()) {
                Text("📝 ${plant.notes}", style = MaterialTheme.typography.bodySmall)
            }

            val canUndo = plant.lastWateredAt?.let { DateUtils.epochDayOf(it) == DateUtils.todayEpochDay() } == true
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onWater) {
                    Icon(Icons.Filled.WaterDrop, contentDescription = null)
                    Text(" Reguei agora")
                }
                if (canUndo) {
                    OutlinedButton(onClick = onUndoWater) {
                        Icon(Icons.Filled.Undo, contentDescription = null)
                        Text(" Desfazer")
                    }
                }
            }

        }
    }
}
