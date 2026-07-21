package com.tassiolima.regavaranda.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tassiolima.regavaranda.data.model.AiProvider
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.di.ServiceLocator
import com.tassiolima.regavaranda.util.BackupManager
import com.tassiolima.regavaranda.util.ImportSummary
import com.tassiolima.regavaranda.util.PhotoAnalyzer
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
    private val photoAnalyzer = PhotoAnalyzer(application)

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

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importResult = MutableStateFlow<ImportSummary?>(null)
    val importResult: StateFlow<ImportSummary?> = _importResult.asStateFlow()

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _importError.value = null
            _importResult.value = null
            runCatching { BackupManager.importBackup(getApplication(), uri) }
                .onSuccess { _importResult.value = it }
                .onFailure { _importError.value = it.message ?: "Falha ao importar backup" }
            _isImporting.value = false
        }
    }

    fun onImportResultShown() {
        _importResult.value = null
        _importError.value = null
    }

    private val _isReclassifying = MutableStateFlow(false)
    val isReclassifying: StateFlow<Boolean> = _isReclassifying.asStateFlow()

    private val _reclassifyProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val reclassifyProgress: StateFlow<Pair<Int, Int>?> = _reclassifyProgress.asStateFlow()

    private val _reclassifyResult = MutableStateFlow<Int?>(null)
    val reclassifyResult: StateFlow<Int?> = _reclassifyResult.asStateFlow()

    private val _reclassifyError = MutableStateFlow<String?>(null)
    val reclassifyError: StateFlow<String?> = _reclassifyError.asStateFlow()

    /**
     * Reprocessa a foto mais recente de cada planta já cadastrada, para aplicar retroativamente
     * a categoria automática (e refrescar dicas/espécie) sem o usuário precisar reanalisar
     * planta por planta.
     */
    fun reclassifyExistingPlants() {
        if (apiKeyRepo.getKey(apiKeyRepo.getSelectedProvider()).isNullOrBlank()) {
            _reclassifyError.value = "Configure sua chave de IA acima antes de reclassificar."
            return
        }
        viewModelScope.launch {
            _isReclassifying.value = true
            _reclassifyError.value = null
            _reclassifyResult.value = null
            runCatching {
                photoAnalyzer.reanalyzeExistingPlants { done, total -> _reclassifyProgress.value = done to total }
            }
                .onSuccess { _reclassifyResult.value = it }
                .onFailure { _reclassifyError.value = it.message ?: "Falha ao reclassificar plantas" }
            _reclassifyProgress.value = null
            _isReclassifying.value = false
        }
    }

    fun onReclassifyResultShown() {
        _reclassifyResult.value = null
        _reclassifyError.value = null
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
        // EncryptedSharedPreferences criptografa de forma síncrona — fora da main thread
        // para não travar a digitação (este método roda a cada tecla).
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            apiKeyRepo.setKey(provider, value)
        }
    }
}
