package com.tassiolima.regavaranda.ui.plant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.data.model.PlantCategory
import com.tassiolima.regavaranda.ui.components.OrientationOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPlantScreen(
    plantId: Long,
    onBack: () -> Unit,
    onOpenCompass: () -> Unit,
    pickedOrientationName: String?,
    onPickedOrientationConsumed: () -> Unit,
    viewModel: AddEditPlantViewModel = viewModel()
) {
    LaunchedEffect(plantId) { viewModel.load(plantId) }

    val state by viewModel.uiState.collectAsState()
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onBack()
    }

    LaunchedEffect(pickedOrientationName) {
        pickedOrientationName?.let { name ->
            Orientation.entries.firstOrNull { it.name == name }?.let(viewModel::onOrientationOverrideChange)
            onPickedOrientationConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (plantId == 0L) "Nova planta" else "Editar planta") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Nome da planta") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = state.category.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de planta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false }
                    ) {
                        PlantCategory.entries.forEach { category ->
                            DropdownMenuItem(
                                text = { Text("${category.emoji} ${category.label}") },
                                onClick = {
                                    viewModel.onCategoryChange(category)
                                    categoryMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    "Rega padrão sugerida: a cada ${state.category.baseWateringDays} dia(s). " +
                        state.category.careTip,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            item {
                OutlinedTextField(
                    value = state.customIntervalDays,
                    onValueChange = viewModel::onCustomIntervalChange,
                    label = { Text("Intervalo de rega personalizado em dias (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = { Text("Observações / cuidados especiais / vitaminas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            item {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        "Esta planta está em outro local (ex: fundos da casa)?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = state.hasCustomLocation, onCheckedChange = viewModel::onHasCustomLocationChange)
                }
            }

            if (state.hasCustomLocation) {
                item {
                    Text(
                        "Para qual direção esta planta está voltada?",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                items(Orientation.entries.toList()) { orientation ->
                    OrientationOption(
                        orientation = orientation,
                        selected = state.orientationOverride == orientation,
                        onSelect = viewModel::onOrientationOverrideChange
                    )
                }
                item {
                    OutlinedButton(onClick = onOpenCompass, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Explore, contentDescription = null)
                        Text(" Usar bússola para descobrir a direção")
                    }
                }
                item {
                    OutlinedTextField(
                        value = state.sunHoursOverride,
                        onValueChange = viewModel::onSunHoursOverrideChange,
                        label = { Text("Horas de sol direto neste local (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text("Salvar")
                }
            }
        }
    }
}
