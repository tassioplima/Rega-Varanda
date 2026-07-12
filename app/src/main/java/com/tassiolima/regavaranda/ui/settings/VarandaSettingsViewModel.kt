package com.tassiolima.regavaranda.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tassiolima.regavaranda.data.model.AiProvider
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VarandaSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = ServiceLocator.settingsRepository(application)
    private val apiKeyRepo = ServiceLocator.apiKeyRepository(application)

    val settings: StateFlow<com.tassiolima.regavaranda.data.repository.VarandaSettings?> =
        settingsRepo.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setOrientation(orientation: Orientation) {
        viewModelScope.launch { settingsRepo.setOrientation(orientation) }
    }

    fun setManualSunHours(hours: Float?) {
        viewModelScope.launch { settingsRepo.setManualSunHoursOverride(hours) }
    }

    fun getSelectedProvider(): AiProvider = apiKeyRepo.getSelectedProvider()

    fun setSelectedProvider(provider: AiProvider) {
        apiKeyRepo.setSelectedProvider(provider)
    }

    fun getApiKey(provider: AiProvider): String? = apiKeyRepo.getKey(provider)

    fun setApiKey(provider: AiProvider, value: String) {
        apiKeyRepo.setKey(provider, value)
    }
}
