package com.tassiolima.regavaranda.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tassiolima.regavaranda.data.model.AiProvider
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.di.ServiceLocator
import com.tassiolima.regavaranda.util.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class VarandaSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = ServiceLocator.settingsRepository(application)
    private val apiKeyRepo = ServiceLocator.apiKeyRepository(application)

    val settings: StateFlow<com.tassiolima.regavaranda.data.repository.VarandaSettings?> =
        settingsRepo.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _backupFile = MutableStateFlow<File?>(null)
    val backupFile: StateFlow<File?> = _backupFile.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    fun exportBackup() {
        viewModelScope.launch {
            _isExporting.value = true
            _exportError.value = null
            runCatching { BackupManager.exportBackup(getApplication()) }
                .onSuccess { _backupFile.value = it }
                .onFailure { _exportError.value = it.message ?: "Falha ao exportar backup" }
            _isExporting.value = false
        }
    }

    fun onBackupShared() {
        _backupFile.value = null
    }

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
