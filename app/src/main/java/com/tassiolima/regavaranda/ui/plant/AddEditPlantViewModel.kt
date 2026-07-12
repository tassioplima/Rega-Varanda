package com.tassiolima.regavaranda.ui.plant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.data.model.PlantCategory
import com.tassiolima.regavaranda.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddEditPlantUiState(
    val plantId: Long = 0,
    val name: String = "",
    val category: PlantCategory = PlantCategory.OUTRA,
    val customIntervalDays: String = "",
    val notes: String = "",
    val hasCustomLocation: Boolean = false,
    val orientationOverride: Orientation? = null,
    val sunHoursOverride: String = "",
    val isSaved: Boolean = false
)

class AddEditPlantViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepo = ServiceLocator.plantRepository(application)

    private val _uiState = MutableStateFlow(AddEditPlantUiState())
    val uiState: StateFlow<AddEditPlantUiState> = _uiState.asStateFlow()

    fun load(plantId: Long) {
        if (plantId == 0L) return
        viewModelScope.launch {
            plantRepo.getPlant(plantId)?.let { plant ->
                _uiState.value = AddEditPlantUiState(
                    plantId = plant.id,
                    name = plant.name,
                    category = plant.category,
                    customIntervalDays = plant.customIntervalDays?.toString() ?: "",
                    notes = plant.notes,
                    hasCustomLocation = plant.orientationOverride != null || plant.sunHoursOverride != null,
                    orientationOverride = plant.orientationOverride,
                    sunHoursOverride = plant.sunHoursOverride?.toString() ?: ""
                )
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }

    fun onCategoryChange(value: PlantCategory) {
        _uiState.value = _uiState.value.copy(category = value)
    }

    fun onCustomIntervalChange(value: String) {
        _uiState.value = _uiState.value.copy(customIntervalDays = value.filter { it.isDigit() })
    }

    fun onNotesChange(value: String) {
        _uiState.value = _uiState.value.copy(notes = value)
    }

    fun onHasCustomLocationChange(value: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasCustomLocation = value,
            orientationOverride = if (value) _uiState.value.orientationOverride else null,
            sunHoursOverride = if (value) _uiState.value.sunHoursOverride else ""
        )
    }

    fun onOrientationOverrideChange(value: Orientation) {
        _uiState.value = _uiState.value.copy(orientationOverride = value)
    }

    fun onSunHoursOverrideChange(value: String) {
        _uiState.value = _uiState.value.copy(sunHoursOverride = value.filter { it.isDigit() || it == '.' })
    }

    fun save() {
        val state = _uiState.value
        if (state.name.isBlank()) return
        viewModelScope.launch {
            val plant = PlantEntity(
                id = state.plantId,
                name = state.name.trim(),
                category = state.category,
                customIntervalDays = state.customIntervalDays.toIntOrNull(),
                notes = state.notes.trim(),
                createdAt = System.currentTimeMillis(),
                orientationOverride = if (state.hasCustomLocation) state.orientationOverride else null,
                sunHoursOverride = if (state.hasCustomLocation) state.sunHoursOverride.toFloatOrNull() else null
            )
            val existing = if (state.plantId != 0L) plantRepo.getPlant(state.plantId) else null
            plantRepo.savePlant(existing?.copy(
                name = plant.name,
                category = plant.category,
                customIntervalDays = plant.customIntervalDays,
                notes = plant.notes,
                orientationOverride = plant.orientationOverride,
                sunHoursOverride = plant.sunHoursOverride
            ) ?: plant)
            _uiState.value = state.copy(isSaved = true)
        }
    }
}
