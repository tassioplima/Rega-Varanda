package com.tassiolima.regavaranda.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tassiolima.regavaranda.R
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
    val snackbarHostState = remember { SnackbarHostState() }
    var plantPendingDelete by remember { mutableStateOf<PlantEntity?>(null) }

    val retryLabel = stringResource(R.string.home_retry)
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            val result = snackbarHostState.showSnackbar(message = message, actionLabel = retryLabel)
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.refreshLocationAndWeather(forceRefresh = true)
            } else {
                viewModel.clearError()
            }
        }
    }

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

    plantPendingDelete?.let { plant ->
        AlertDialog(
            onDismissRequest = { plantPendingDelete = null },
            title = { Text(stringResource(R.string.home_delete_dialog_title, plant.name)) },
            text = { Text(stringResource(R.string.home_delete_dialog_body)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePlant(plant)
                    plantPendingDelete = null
                }) {
                    Text(stringResource(R.string.home_delete_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { plantPendingDelete = null }) {
                    Text(stringResource(R.string.home_delete_cancel))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenDashboard) {
                        Icon(Icons.Filled.Favorite, contentDescription = stringResource(R.string.home_health_dashboard))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.home_settings))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddPlant) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.home_add_plant))
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

                else -> HomeContent(
                    state = state,
                    viewModel = viewModel,
                    onOpenPlant = onOpenPlant,
                    onAddPlant = onAddPlant,
                    onRequestDelete = { plantPendingDelete = it }
                )
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
            stringResource(R.string.home_permission_rationale),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.size(16.dp))
        Button(onClick = onRequestPermission) { Text(stringResource(R.string.home_permission_button)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    viewModel: HomeViewModel,
    onOpenPlant: (Long) -> Unit,
    onAddPlant: () -> Unit,
    onRequestDelete: (PlantEntity) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.refreshLocationAndWeather(forceRefresh = true) },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.weather != null && state.sunExposure != null) {
                item { WeatherCard(state) }
            }

            if (state.dailyTips.isNotEmpty()) {
                item { TipsCard(state.dailyTips) }
            }

            if (state.plantsWithPlans.isEmpty()) {
                item { EmptyGardenCard(onAddPlant) }
            }

            items(state.plantsWithPlans, key = { it.plant.id }) { pwp ->
                PlantCard(
                    pwp = pwp,
                    onWater = { viewModel.markWatered(pwp.plant) },
                    onUndoWater = { viewModel.undoWatering(pwp.plant) },
                    onDelete = { onRequestDelete(pwp.plant) },
                    onOpenDetail = { onOpenPlant(pwp.plant.id) }
                )
            }
        }
    }
}

@Composable
private fun EmptyGardenCard(onAddPlant: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("🪴", style = MaterialTheme.typography.displayLarge)
            Text(
                stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                stringResource(R.string.home_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAddPlant) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(" " + stringResource(R.string.home_empty_cta))
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
            Text(stringResource(R.string.home_weather_title), style = MaterialTheme.typography.titleMedium)
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
            Text(stringResource(R.string.home_tips_title), style = MaterialTheme.typography.titleMedium)
            tips.forEach { tip -> Text(tip, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun WateringStatusBadge(pwp: PlantWithPlan) {
    val (label, container, content) = when {
        pwp.status.isDueNow && pwp.plan.timesPerDay >= 2 -> Triple(
            "🚨 Regar agora (${pwp.status.timesRemainingToday}x hoje)",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        pwp.status.isDueNow -> Triple(
            stringResource(R.string.home_status_water_now),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        pwp.status.nextDueAtMillis != null -> {
            val remaining = pwp.status.nextDueAtMillis!! - System.currentTimeMillis()
            Triple(
                "⏳ Rega em ${WateringScheduler.formatCountdown(remaining)}",
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        else -> Triple(
            stringResource(R.string.home_status_watered_today),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }

    Surface(color = container, contentColor = content, shape = RoundedCornerShape(8.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
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
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (pwp.latestPhotoPath != null) {
                    AsyncImage(
                        model = pwp.latestPhotoPath,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(plant.category.emoji, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(plant.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        plant.identifiedSpecies?.takeIf { it.isNotBlank() } ?: plant.category.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    WateringStatusBadge(pwp)
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.home_delete_plant),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                pwp.plan.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (pwp.sunExposure.isPlantSpecificLocation) {
                Text(
                    "📍 Local próprio: ${pwp.sunExposure.orientation?.label ?: "sol manual"} " +
                        "(~${pwp.sunExposure.estimatedSunHours.roundToInt()}h de sol)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                    Text(stringResource(R.string.home_watered_now))
                }
                if (canUndo) {
                    OutlinedButton(onClick = onUndoWater) {
                        Icon(Icons.Filled.Undo, contentDescription = null)
                        Text(stringResource(R.string.home_undo))
                    }
                }
            }
        }
    }
}
