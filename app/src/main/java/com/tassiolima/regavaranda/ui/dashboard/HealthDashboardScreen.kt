package com.tassiolima.regavaranda.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.tassiolima.regavaranda.data.model.HealthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDashboardScreen(
    onBack: () -> Unit,
    onOpenPlant: (Long) -> Unit,
    viewModel: HealthDashboardViewModel = viewModel()
) {
    val summaries by viewModel.summaries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saúde das plantas") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        if (summaries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp)
            ) {
                Text("Adicione plantas e fotos para acompanhar a saúde delas aqui.")
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SummaryHeader(summaries) }

            items(summaries, key = { it.plant.id }) { summary ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPlant(summary.plant.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (summary.latestPhoto != null) {
                            AsyncImage(
                                model = summary.latestPhoto.filePath,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Text(summary.plant.category.emoji, style = MaterialTheme.typography.headlineMedium)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(summary.plant.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                summary.currentHealth?.let { "${it.emoji} ${it.label}" }
                                    ?: if (summary.latestPhoto != null) "🔎 Aguardando avaliação de saúde" else "📷 Sem fotos ainda",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(trendLabel(summary.trend), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(summaries: List<PlantHealthSummary>) {
    val healthy = summaries.count { it.currentHealth == HealthState.SAUDAVEL }
    val attention = summaries.count { it.currentHealth == HealthState.ATENCAO }
    val critical = summaries.count { it.currentHealth == HealthState.CRITICA }
    val noPhotos = summaries.count { it.currentHealth == null && it.latestPhoto == null }
    val pendingAssessment = summaries.count { it.currentHealth == null && it.latestPhoto != null }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Resumo", style = MaterialTheme.typography.titleMedium)
            Text("💚 Saudáveis: $healthy   🟡 Atenção: $attention   🔴 Críticas: $critical")
            if (pendingAssessment > 0) {
                Text("🔎 Aguardando avaliação: $pendingAssessment", style = MaterialTheme.typography.bodySmall)
            }
            if (noPhotos > 0) {
                Text("📷 Sem fotos ainda: $noPhotos", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun trendLabel(trend: HealthTrend): String = when (trend) {
    HealthTrend.MELHORANDO -> "📈 Melhorando"
    HealthTrend.PIORANDO -> "📉 Piorando"
    HealthTrend.ESTAVEL -> "➡️ Estável"
    HealthTrend.DESCONHECIDA -> "Tire mais fotos para ver a evolução"
}
