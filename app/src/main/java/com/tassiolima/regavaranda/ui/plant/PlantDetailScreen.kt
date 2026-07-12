package com.tassiolima.regavaranda.ui.plant

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tassiolima.regavaranda.data.local.AnalysisStatus
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.PlantPhotoEntity
import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.domain.WateringScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: Long,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onEditPlant: () -> Unit,
    onOpenChat: () -> Unit,
    viewModel: PlantDetailViewModel = viewModel()
) {
    LaunchedEffect(plantId) { viewModel.load(plantId) }

    val context = LocalContext.current
    val plant by viewModel.plant.collectAsState()
    val photos by viewModel.photos.collectAsState()

    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFile by remember { mutableStateOf<java.io.File?>(null) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = pendingFile
        if (success && file != null) {
            viewModel.onPhotoCaptured(plantId, file)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onGalleryPhotoPicked(context, plantId, it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(plant?.name ?: "Planta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onEditPlant) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar planta")
                    }
                }
            )
        },
        floatingActionButton = {
            Box {
                FloatingActionButton(onClick = { fabMenuExpanded = true }) {
                    Icon(Icons.Filled.AddAPhoto, contentDescription = "Adicionar foto")
                }
                DropdownMenu(expanded = fabMenuExpanded, onDismissRequest = { fabMenuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Tirar foto") },
                        leadingIcon = { Icon(Icons.Filled.CameraAlt, contentDescription = null) },
                        onClick = {
                            fabMenuExpanded = false
                            val (file, uri) = viewModel.prepareNewPhotoFile(context, plantId)
                            pendingFile = file
                            pendingUri = uri
                            cameraLauncher.launch(uri)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Escolher da galeria") },
                        leadingIcon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = null) },
                        onClick = {
                            fabMenuExpanded = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            plant?.let { p ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("${p.category.emoji} ${p.category.label}", style = MaterialTheme.typography.titleMedium)
                            p.identifiedSpecies?.takeIf { it.isNotBlank() }?.let { species ->
                                Text("🔬 Espécie identificada pela IA: $species", style = MaterialTheme.typography.bodySmall)
                            }
                            p.aiWateringIntervalDays?.let { days ->
                                Text(
                                    "💧 Intervalo de rega recomendado para esta espécie: a cada $days dia(s)",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (p.notes.isNotBlank()) {
                                Text(p.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                item {
                    MoistureCard(plant = p, onRecordMoisture = viewModel::recordMoistureReading)
                }
            }

            if (photos.size >= 2) {
                item { BeforeAfterCard(oldest = photos.last(), newest = photos.first()) }
            }

            val hasAnalyzedPhoto = photos.any { it.analysisStatus == AnalysisStatus.DONE }
            if (hasAnalyzedPhoto) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tire suas dúvidas sobre esta planta", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Converse com a IA sobre ${plant?.name ?: "esta planta"} usando o diagnóstico mais recente como contexto.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(onClick = onOpenChat, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Filled.Chat, contentDescription = null)
                                Text(" Abrir chat da planta")
                            }
                        }
                    }
                }
            }

            if (photos.isEmpty()) {
                item {
                    Text(
                        "Ainda não há fotos desta planta. Toque no botão + para tirar uma foto ou " +
                            "escolher uma da galeria e receber dicas de cuidado.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            items(photos, key = { it.id }) { photo ->
                PhotoCard(
                    photo = photo,
                    onConfirmHealth = { state -> viewModel.confirmHealth(photo, state) },
                    onRetryAnalysis = { viewModel.analyze(photo.id) },
                    onDelete = { viewModel.deletePhoto(photo) },
                    onOpenSettings = onOpenSettings
                )
            }
        }
    }
}

@Composable
private fun MoistureCard(plant: PlantEntity, onRecordMoisture: (Int) -> Unit) {
    val threshold = plant.category.dryMoistureThreshold
    var sliderValue by remember { mutableFloatStateOf((plant.soilMoistureLevel ?: threshold).toFloat()) }
    val level = sliderValue.roundToInt()

    val (zoneLabel, zoneColor) = when {
        level <= threshold -> "🌵 Seco" to MaterialTheme.colorScheme.error
        level <= threshold + 2 -> "🌱 Úmido" to MaterialTheme.colorScheme.primary
        else -> "💧 Molhado" to MaterialTheme.colorScheme.tertiary
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("🧪 Umidade do solo", style = MaterialTheme.typography.titleMedium)
                Text(
                    "$level/10 · $zoneLabel",
                    style = MaterialTheme.typography.titleMedium,
                    color = zoneColor
                )
            }

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 1f..10f,
                steps = 8
            )

            Button(
                onClick = { onRecordMoisture(level) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Science, contentDescription = null)
                Text(" Registrar leitura")
            }

            val feedback = remember(plant.soilMoistureLevel, plant.soilMoistureReadingAt) {
                WateringScheduler.moistureFeedback(plant, System.currentTimeMillis())
            }
            feedback?.let {
                Text(
                    it.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.suggestsWatering) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            } ?: Text(
                "Ainda não há leitura registrada. Ajuste o medidor de umidade no solo, mova o controle " +
                    "acima até o valor que ele mostrar e toque em Registrar.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun BeforeAfterCard(oldest: PlantPhotoEntity, newest: PlantPhotoEntity) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")) }
    val daysBetween = ((newest.takenAt - oldest.takenAt) / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("📸 Antes e depois", style = MaterialTheme.typography.titleMedium)
            Text(
                if (daysBetween > 0) "$daysBetween dia(s) de diferença" else "Mesmo dia",
                style = MaterialTheme.typography.bodySmall
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Antes · ${dateFormat.format(Date(oldest.takenAt))}", style = MaterialTheme.typography.labelSmall)
                    AsyncImage(
                        model = oldest.filePath,
                        contentDescription = "Foto mais antiga",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Depois · ${dateFormat.format(Date(newest.takenAt))}", style = MaterialTheme.typography.labelSmall)
                    AsyncImage(
                        model = newest.filePath,
                        contentDescription = "Foto mais recente",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoCard(
    photo: PlantPhotoEntity,
    onConfirmHealth: (HealthState) -> Unit,
    onRetryAnalysis: () -> Unit,
    onDelete: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateFormat.format(Date(photo.takenAt)), style = MaterialTheme.typography.bodySmall)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remover foto")
                }
            }

            AsyncImage(
                model = photo.filePath,
                contentDescription = "Foto da planta",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )

            when (photo.analysisStatus) {
                AnalysisStatus.PENDING, AnalysisStatus.ANALYZING -> Row(
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Text("Analisando foto com IA...", style = MaterialTheme.typography.bodySmall)
                }

                AnalysisStatus.NO_API_KEY -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Configure a chave de IA nas configurações para receber diagnóstico e dicas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(onClick = onOpenSettings) { Text("Ir para configurações") }
                }

                AnalysisStatus.FAILED -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Não foi possível analisar esta foto${photo.diagnosis?.let { ": $it" } ?: "."}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = onRetryAnalysis) { Text("Tentar novamente") }
                }

                AnalysisStatus.DONE -> DoneAnalysisContent(photo, onConfirmHealth)
            }
        }
    }
}

@Composable
private fun DoneAnalysisContent(photo: PlantPhotoEntity, onConfirmHealth: (HealthState) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        photo.diagnosis?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        photo.identifiedSpecies?.takeIf { it.isNotBlank() }?.let {
            Text("🔬 Espécie identificada: $it", style = MaterialTheme.typography.bodySmall)
        }
        photo.recommendedWateringIntervalDays?.let {
            Text("💧 Intervalo de rega ideal para esta espécie: a cada $it dia(s)", style = MaterialTheme.typography.bodySmall)
        }
        photo.evolutionNote?.takeIf { it.isNotBlank() }?.let {
            Text(
                "📈 Evolução: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        val suggested = photo.aiSuggestedHealth
        val confirmed = photo.confirmedHealth
        if (suggested != null) {
            Text(
                "Estado sugerido pela IA: ${suggested.emoji} ${suggested.label}" +
                    if (confirmed != null) " · Confirmado: ${confirmed.emoji} ${confirmed.label}" else "",
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (confirmed == null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthState.entries.forEach { state ->
                    AssistChip(
                        onClick = { onConfirmHealth(state) },
                        label = { Text("${state.emoji} ${state.label}") }
                    )
                }
            }
        }

        photo.wateringTip?.takeIf { it.isNotBlank() }?.let { Text("💧 $it", style = MaterialTheme.typography.bodySmall) }
        photo.pruningTip?.takeIf { it.isNotBlank() }?.let { Text("✂️ $it", style = MaterialTheme.typography.bodySmall) }
        photo.fertilizingTip?.takeIf { it.isNotBlank() }?.let { Text("💊 $it", style = MaterialTheme.typography.bodySmall) }
        photo.repottingTip?.takeIf { it.isNotBlank() }?.let { Text("🪴 $it", style = MaterialTheme.typography.bodySmall) }
    }
}
