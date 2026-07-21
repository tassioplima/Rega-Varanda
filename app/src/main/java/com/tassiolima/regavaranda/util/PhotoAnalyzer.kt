package com.tassiolima.regavaranda.util

import android.content.Context
import com.tassiolima.regavaranda.data.local.AnalysisStatus
import com.tassiolima.regavaranda.di.ServiceLocator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Chama a IA de visão para uma foto já salva e aplica o resultado (diagnóstico, dicas,
 * espécie, categoria, intervalo de rega) na foto e na planta. Compartilhado entre a análise
 * de uma foto nova/individual e a reclassificação em lote de plantas já cadastradas, para que
 * as duas usem exatamente a mesma lógica.
 */
class PhotoAnalyzer(context: Context) {

    private val plantRepo = ServiceLocator.plantRepository(context)
    private val photoRepo = ServiceLocator.plantPhotoRepository(context)
    private val apiKeyRepo = ServiceLocator.apiKeyRepository(context)

    suspend fun analyze(photoId: Long) {
        val photo = photoRepo.getById(photoId) ?: return
        val plant = plantRepo.getPlant(photo.plantId) ?: return
        val provider = apiKeyRepo.getSelectedProvider()
        val apiKey = apiKeyRepo.getKey(provider)
        if (apiKey.isNullOrBlank()) {
            photoRepo.update(photo.copy(analysisStatus = AnalysisStatus.NO_API_KEY))
            return
        }

        photoRepo.update(photo.copy(analysisStatus = AnalysisStatus.ANALYZING))
        try {
            val base64 = withContext(Dispatchers.IO) { ImageUtils.toBase64Jpeg(File(photo.filePath)) }
            val historySummary = buildHistorySummary(plant.id, excludingPhotoId = photo.id)
            val visionClient = ServiceLocator.visionClientFor(provider)
            val result = visionClient.analyzePhoto(
                apiKey,
                base64,
                plant.name,
                plant.category,
                plant.notes,
                historySummary
            )

            photoRepo.update(
                photo.copy(
                    analysisStatus = AnalysisStatus.DONE,
                    aiSuggestedHealth = result.healthState,
                    diagnosis = result.diagnosis,
                    wateringTip = result.wateringTip,
                    pruningTip = result.pruningTip,
                    fertilizingTip = result.fertilizingTip,
                    repottingTip = result.repottingTip,
                    identifiedSpecies = result.identifiedSpecies.takeIf { it.isNotBlank() },
                    recommendedWateringIntervalDays = result.recommendedWateringIntervalDays,
                    evolutionNote = result.evolutionNote.takeIf { it.isNotBlank() }
                )
            )

            if (result.identifiedSpecies.isNotBlank() ||
                result.recommendedWateringIntervalDays != null ||
                result.suggestedCategory != null
            ) {
                val latestPlant = plantRepo.getPlant(plant.id) ?: plant
                plantRepo.savePlant(
                    latestPlant.copy(
                        identifiedSpecies = result.identifiedSpecies.takeIf { it.isNotBlank() } ?: latestPlant.identifiedSpecies,
                        aiWateringIntervalDays = result.recommendedWateringIntervalDays ?: latestPlant.aiWateringIntervalDays,
                        category = result.suggestedCategory ?: latestPlant.category
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            photoRepo.update(photo.copy(analysisStatus = AnalysisStatus.FAILED, diagnosis = e.message))
        }
    }

    /**
     * Reclassifica todas as plantas que já têm ao menos uma foto analisada, reaproveitando a
     * foto mais recente já salva de cada uma — sem precisar tirar uma foto nova. Útil para
     * aplicar retroativamente uma melhoria na análise (ex.: categoria automática) a plantas
     * cadastradas antes dela existir. Retorna quantas plantas foram reclassificadas.
     */
    suspend fun reanalyzeExistingPlants(onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }): Int {
        val plants = plantRepo.observePlants().first()
        var processed = 0
        plants.forEachIndexed { index, plant ->
            val latestAnalyzed = photoRepo.observeForPlant(plant.id).first()
                .filter { it.analysisStatus == AnalysisStatus.DONE }
                .maxByOrNull { it.takenAt }
            if (latestAnalyzed != null) {
                analyze(latestAnalyzed.id)
                processed++
            }
            onProgress(index + 1, plants.size)
        }
        return processed
    }

    /** Resumo cronológico das análises anteriores desta planta, para dar contexto de evolução à IA. */
    private suspend fun buildHistorySummary(plantId: Long, excludingPhotoId: Long): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
        val previousPhotos = photoRepo.observeForPlant(plantId).first()
            .filter { it.id != excludingPhotoId && it.analysisStatus == AnalysisStatus.DONE }
            .sortedBy { it.takenAt }
            .takeLast(5)

        if (previousPhotos.isEmpty()) return ""

        return previousPhotos.joinToString("\n") { photo ->
            val date = dateFormat.format(Date(photo.takenAt))
            val health = photo.confirmedHealth?.label ?: photo.aiSuggestedHealth?.label ?: "?"
            "- $date: $health — ${photo.diagnosis.orEmpty()}"
        }
    }
}
