package com.tassiolima.regavaranda.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tassiolima.regavaranda.data.model.AiProvider
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.ui.components.OrientationOption
import com.tassiolima.regavaranda.util.ImageUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VarandaSettingsScreen(
    onBack: () -> Unit,
    onOpenCompass: () -> Unit,
    pickedOrientationName: String?,
    onPickedOrientationConsumed: () -> Unit,
    viewModel: VarandaSettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    var manualHoursText by remember(settings) {
        mutableStateOf(settings?.manualSunHoursOverride?.toString() ?: "")
    }
    var selectedProvider by remember { mutableStateOf(viewModel.getSelectedProvider()) }
    var apiKeyText by remember(selectedProvider) { mutableStateOf(viewModel.getApiKey(selectedProvider) ?: "") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    val isExporting by viewModel.isExporting.collectAsState()
    val backupFile by viewModel.backupFile.collectAsState()
    val exportError by viewModel.exportError.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importResult by viewModel.importResult.collectAsState()
    val importError by viewModel.importError.collectAsState()
    val isReclassifying by viewModel.isReclassifying.collectAsState()
    val reclassifyProgress by viewModel.reclassifyProgress.collectAsState()
    val reclassifyResult by viewModel.reclassifyResult.collectAsState()
    val reclassifyError by viewModel.reclassifyError.collectAsState()
    val context = LocalContext.current

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importBackup(it) }
    }

    LaunchedEffect(pickedOrientationName) {
        pickedOrientationName?.let { name ->
            Orientation.entries.firstOrNull { it.name == name }?.let(viewModel::setOrientation)
            onPickedOrientationConsumed()
        }
    }

    LaunchedEffect(backupFile) {
        backupFile?.let { file ->
            val uri = ImageUtils.uriForFile(context, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar backup"))
            viewModel.onBackupShared()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuração da varanda") },
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
                Text(
                    "Para qual direção sua varanda / a frente da casa está voltada?",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(Orientation.entries.toList()) { orientation ->
                OrientationOption(
                    orientation = orientation,
                    selected = settings?.orientation == orientation,
                    onSelect = viewModel::setOrientation
                )
            }
            item {
                OutlinedButton(onClick = onOpenCompass, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Explore, contentDescription = null)
                    Text(" Usar bússola para descobrir a direção")
                }
            }

            item {
                Text(
                    "Opcional: se você já sabe quantas horas de sol direto sua varanda recebe " +
                        "num dia de céu limpo, informe aqui para uma estimativa mais precisa.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                OutlinedTextField(
                    value = manualHoursText,
                    onValueChange = { value ->
                        manualHoursText = value.filter { it.isDigit() || it == '.' }
                        viewModel.setManualSunHours(manualHoursText.toFloatOrNull())
                    },
                    label = { Text("Horas de sol direto (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    "Análise de fotos com IA",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            item {
                Text(
                    "Escolha qual IA analisa as fotos das plantas e informe a chave correspondente. " +
                        "Cada chave é guardada de forma criptografada só neste aparelho e é usada apenas " +
                        "para chamar a API do provedor escolhido, diretamente do seu celular.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            items(AiProvider.entries.toList()) { provider ->
                val selected = selectedProvider == provider
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = selected, onClick = {
                            selectedProvider = provider
                            viewModel.setSelectedProvider(provider)
                        })
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected, onClick = {
                            selectedProvider = provider
                            viewModel.setSelectedProvider(provider)
                        })
                        Text(provider.label)
                    }
                }
            }
            item {
                Text(
                    "Gere sua chave em ${selectedProvider.consoleUrl}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { value ->
                        apiKeyText = value
                        viewModel.setApiKey(selectedProvider, value)
                    },
                    label = { Text(selectedProvider.keyHint) },
                    singleLine = true,
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (apiKeyVisible) "Ocultar chave" else "Mostrar chave"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text(
                    "Já tem plantas cadastradas antes desta função existir? Reclassifique todas de " +
                        "uma vez (suculenta, orquídea, folhagem tropical...) reaproveitando a última " +
                        "foto já analisada de cada uma — sem precisar tirar foto de novo.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                OutlinedButton(
                    onClick = viewModel::reclassifyExistingPlants,
                    enabled = !isReclassifying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isReclassifying) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    }
                    val progressLabel = reclassifyProgress?.let { (done, total) -> " Reclassificando $done/$total..." }
                    Text(progressLabel ?: " Reclassificar plantas cadastradas")
                }
            }
            reclassifyResult?.let { count ->
                item {
                    Text(
                        if (count > 0) "✅ $count planta(s) reclassificada(s)." else "Nenhuma planta com foto analisada para reclassificar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            reclassifyError?.let { error ->
                item {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Text("Backup", style = MaterialTheme.typography.titleMedium)
            }
            item {
                Text(
                    "Exporte suas plantas, fotos, histórico de rega e conversas em um arquivo .zip " +
                        "para guardar ou transferir para outro aparelho.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                OutlinedButton(
                    onClick = viewModel::exportBackup,
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Filled.Save, contentDescription = null)
                    }
                    Text(" Exportar backup")
                }
            }
            exportError?.let { error ->
                item {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            item {
                Text(
                    "Importar um backup adiciona as plantas do arquivo às que você já tem " +
                        "(não substitui nem apaga nada existente).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            item {
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    enabled = !isImporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Filled.FileUpload, contentDescription = null)
                    }
                    Text(" Importar backup")
                }
            }
            importResult?.let { result ->
                item {
                    Text(
                        "✅ Importado: ${result.plantsImported} planta(s), ${result.photosImported} foto(s), " +
                            "${result.chatMessagesImported} mensagem(ns) de chat, ${result.wateringLogImported} " +
                            "registro(s) de rega.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            importError?.let { error ->
                item {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
