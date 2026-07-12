package com.tassiolima.regavaranda.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.WateringLogEntity
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.data.remote.WeatherSnapshot
import com.tassiolima.regavaranda.data.repository.VarandaSettings
import com.tassiolima.regavaranda.di.ServiceLocator
import com.tassiolima.regavaranda.domain.PlantCareAdvisor
import com.tassiolima.regavaranda.domain.PlantWithPlan
import com.tassiolima.regavaranda.domain.SunExposure
import com.tassiolima.regavaranda.domain.SunExposureCalculator
import com.tassiolima.regavaranda.domain.WateringScheduler
import com.tassiolima.regavaranda.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val hasLocationPermission: Boolean = true,
    val weather: WeatherSnapshot? = null,
    val sunExposure: SunExposure? = null,
    val orientation: Orientation? = null,
    val plantsWithPlans: List<PlantWithPlan> = emptyList(),
    val dailyTips: List<String> = emptyList(),
    val errorMessage: String? = null
)

private data class CoreState(
    val settings: VarandaSettings,
    val plants: List<PlantEntity>,
    val weather: WeatherSnapshot?,
    val loading: Boolean,
    val error: String?
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val plantRepo = ServiceLocator.plantRepository(application)
    private val settingsRepo = ServiceLocator.settingsRepository(application)
    private val locationProvider = ServiceLocator.locationProvider(application)
    private val weatherClient = ServiceLocator.weatherClient()

    private val weatherFlow = MutableStateFlow<WeatherSnapshot?>(null)
    private val loadingFlow = MutableStateFlow(true)
    private val errorFlow = MutableStateFlow<String?>(null)

    private val coreState = combine(
        settingsRepo.settingsFlow,
        plantRepo.observePlants(),
        weatherFlow,
        loadingFlow,
        errorFlow
    ) { settings, plants, weather, loading, error ->
        CoreState(settings, plants, weather, loading, error)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        coreState,
        plantRepo.observeWateringLog()
    ) { core, wateringLog ->
        buildUiState(core, wateringLog)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refreshLocationAndWeather()
    }

    private fun buildUiState(core: CoreState, wateringLog: List<WateringLogEntity>): HomeUiState {
        val (settings, plants, weather, loading, error) = core
        val hasPermission = locationProvider.hasLocationPermission()
        if (weather == null) {
            return HomeUiState(
                isLoading = loading,
                hasLocationPermission = hasPermission,
                orientation = settings.orientation,
                errorMessage = error
            )
        }

        val sunExposure = SunExposureCalculator.estimate(weather, settings.orientation, settings.manualSunHoursOverride)
        val now = System.currentTimeMillis()
        val logsByPlant = wateringLog.groupBy { it.plantId }
        val plansWithStatus = plants.map { plant ->
            val plantSunExposure = SunExposureCalculator.estimateForPlant(weather, settings, plant)
            val plan = WateringScheduler.computePlan(
                plant,
                weather,
                plantSunExposure.estimatedSunHours
            )
            val status = WateringScheduler.computeStatus(plan, plant, now)
            val overwateringWarning = WateringScheduler.detectOverwatering(
                plan,
                logsByPlant[plant.id].orEmpty(),
                now
            )
            val moistureFeedback = WateringScheduler.moistureFeedback(plant, now)
            PlantWithPlan(plant, plan, status, plantSunExposure, overwateringWarning, moistureFeedback)
        }
        val tips = PlantCareAdvisor.dailyTips(weather, sunExposure, plansWithStatus, now)

        return HomeUiState(
            isLoading = loading,
            hasLocationPermission = hasPermission,
            weather = weather,
            sunExposure = sunExposure,
            orientation = settings.orientation,
            plantsWithPlans = plansWithStatus,
            dailyTips = tips,
            errorMessage = error
        )
    }

    fun refreshLocationAndWeather() {
        viewModelScope.launch {
            loadingFlow.value = true
            errorFlow.value = null
            val startedAt = System.currentTimeMillis()
            try {
                val location = locationProvider.getCurrentLocation()
                if (location == null) {
                    errorFlow.value = "Não foi possível obter a localização. Verifique o GPS e as permissões."
                    return@launch
                }
                settingsRepo.setLastLocation(location.latitude, location.longitude)
                weatherFlow.value = weatherClient.fetchToday(location.latitude, location.longitude)
            } catch (e: Exception) {
                errorFlow.value = "Erro ao buscar o clima: ${e.message}"
            } finally {
                // Garante que o indicador de "atualizando" fique visível por tempo suficiente
                // para ser percebido, mesmo quando o GPS/clima respondem quase instantaneamente.
                val elapsed = System.currentTimeMillis() - startedAt
                val minVisibleMs = 700L
                if (elapsed < minVisibleMs) delay(minVisibleMs - elapsed)
                loadingFlow.value = false
            }
        }
    }

    fun markWatered(plant: PlantEntity) {
        viewModelScope.launch {
            plantRepo.markWatered(plant, System.currentTimeMillis(), DateUtils.todayEpochDay())
        }
    }

    fun undoWatering(plant: PlantEntity) {
        viewModelScope.launch {
            plantRepo.undoLastWatering(plant, DateUtils.todayEpochDay())
        }
    }

    fun markFertilized(plant: PlantEntity) {
        viewModelScope.launch {
            plantRepo.markFertilized(plant, System.currentTimeMillis())
        }
    }

    fun recordMoistureReading(plant: PlantEntity, level: Int) {
        viewModelScope.launch {
            plantRepo.recordMoistureReading(plant, level, System.currentTimeMillis())
        }
    }

    fun deletePlant(plant: PlantEntity) {
        viewModelScope.launch { plantRepo.deletePlant(plant) }
    }
}
