package com.tassiolima.regavaranda.util

import android.content.Context
import android.net.Uri
import com.tassiolima.regavaranda.data.local.AnalysisStatus
import com.tassiolima.regavaranda.data.local.ChatMessageEntity
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.PlantPhotoEntity
import com.tassiolima.regavaranda.data.local.WateringLogEntity
import com.tassiolima.regavaranda.data.model.ChatRole
import com.tassiolima.regavaranda.data.model.HealthState
import com.tassiolima.regavaranda.data.model.Orientation
import com.tassiolima.regavaranda.data.model.PlantCategory
import com.tassiolima.regavaranda.di.ServiceLocator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class ImportSummary(
    val plantsImported: Int,
    val photosImported: Int,
    val chatMessagesImported: Int,
    val wateringLogImported: Int
)

/** Exporta/importa plantas, fotos, histórico de rega e chats via um único arquivo .zip. */
object BackupManager {

    suspend fun exportBackup(context: Context): File = withContext(Dispatchers.IO) {
        val plantRepo = ServiceLocator.plantRepository(context)
        val photoRepo = ServiceLocator.plantPhotoRepository(context)
        val chatRepo = ServiceLocator.chatRepository(context)

        val plants = plantRepo.observePlants().first()
        val photos = photoRepo.observeAll().first()
        val wateringLog = plantRepo.observeWateringLog().first()
        val chatMessages = chatRepo.getAll()

        val json = JSONObject().apply {
            put("exportedAt", System.currentTimeMillis())
            put("plants", JSONArray(plants.map { it.toJson() }))
            put("photos", JSONArray(photos.map { it.toJson(context) }))
            put("wateringLog", JSONArray(wateringLog.map { it.toJson() }))
            put("chatMessages", JSONArray(chatMessages.map { it.toJson() }))
        }

        val backupsDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US).format(Date())
        val zipFile = File(backupsDir, "regavaranda_backup_$stamp.zip")

        ZipOutputStream(zipFile.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(json.toString(2).toByteArray())
            zip.closeEntry()

            photos.forEach { photo ->
                val file = File(photo.filePath)
                if (file.exists()) {
                    zip.putNextEntry(ZipEntry("photos/${photoEntryName(context, photo)}"))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        zipFile
    }

    /**
     * Importa um backup .zip como um NOVO conjunto de plantas — soma às plantas já existentes
     * em vez de substituí-las, então IDs antigos do arquivo são remapeados para novos IDs ao
     * inserir. Rodar a importação do mesmo backup mais de uma vez duplica as plantas.
     */
    suspend fun importBackup(context: Context, zipUri: Uri): ImportSummary = withContext(Dispatchers.IO) {
        val plantRepo = ServiceLocator.plantRepository(context)
        val photoRepo = ServiceLocator.plantPhotoRepository(context)
        val chatRepo = ServiceLocator.chatRepository(context)

        val tempZip = File.createTempFile("import_", ".zip", context.cacheDir)
        try {
            context.contentResolver.openInputStream(zipUri)?.use { input ->
                tempZip.outputStream().use { output -> input.copyTo(output) }
            } ?: throw java.io.IOException("Não foi possível ler o arquivo selecionado")

            ZipFile(tempZip).use { zip ->
                val jsonEntry = zip.getEntry("backup.json")
                    ?: throw java.io.IOException("Arquivo não parece ser um backup do Rega Varanda")
                val json = JSONObject(zip.getInputStream(jsonEntry).bufferedReader().readText())

                val oldToNewPlantId = mutableMapOf<Long, Long>()
                val plantsJson = json.getJSONArray("plants")
                for (i in 0 until plantsJson.length()) {
                    val p = plantsJson.getJSONObject(i)
                    val newId = plantRepo.insertPlant(p.toPlantEntity())
                    oldToNewPlantId[p.getLong("id")] = newId
                }

                val photosJson = json.optJSONArray("photos") ?: JSONArray()
                var photosImported = 0
                for (i in 0 until photosJson.length()) {
                    val ph = photosJson.getJSONObject(i)
                    val newPlantId = oldToNewPlantId[ph.getLong("plantId")] ?: continue
                    val relativePath = ph.getString("relativePath")
                    val fileName = File(relativePath).name
                    val destFile = File(ImageUtils.photosDir(context, newPlantId), fileName)

                    zip.getEntry("photos/$relativePath")?.let { entry ->
                        zip.getInputStream(entry).use { input -> destFile.outputStream().use { input.copyTo(it) } }
                    }

                    photoRepo.insert(ph.toPlantPhotoEntity(newPlantId, destFile.absolutePath))
                    photosImported++
                }

                val chatJson = json.optJSONArray("chatMessages") ?: JSONArray()
                var chatImported = 0
                for (i in 0 until chatJson.length()) {
                    val c = chatJson.getJSONObject(i)
                    val newPlantId = oldToNewPlantId[c.getLong("plantId")] ?: continue
                    chatRepo.insert(
                        ChatMessageEntity(
                            plantId = newPlantId,
                            role = ChatRole.valueOf(c.getString("role")),
                            content = c.getString("content"),
                            createdAt = c.getLong("createdAt")
                        )
                    )
                    chatImported++
                }

                val logJson = json.optJSONArray("wateringLog") ?: JSONArray()
                var logImported = 0
                for (i in 0 until logJson.length()) {
                    val w = logJson.getJSONObject(i)
                    val newPlantId = oldToNewPlantId[w.getLong("plantId")] ?: continue
                    plantRepo.insertWateringLogEntry(newPlantId, w.getLong("wateredAt"))
                    logImported++
                }

                ImportSummary(
                    plantsImported = oldToNewPlantId.size,
                    photosImported = photosImported,
                    chatMessagesImported = chatImported,
                    wateringLogImported = logImported
                )
            }
        } finally {
            tempZip.delete()
        }
    }

    private fun JSONObject.longOrNull(key: String): Long? = if (isNull(key)) null else optLong(key)
    private fun JSONObject.intOrNull(key: String): Int? = if (isNull(key)) null else optInt(key)
    private fun JSONObject.stringOrNull(key: String): String? = if (isNull(key)) null else optString(key)
    private fun JSONObject.floatOrNull(key: String): Float? = if (isNull(key)) null else optDouble(key).toFloat()

    private fun JSONObject.toPlantEntity() = PlantEntity(
        name = getString("name"),
        category = PlantCategory.valueOf(getString("category")),
        customIntervalDays = intOrNull("customIntervalDays"),
        notes = optString("notes", ""),
        createdAt = getLong("createdAt"),
        lastWateredAt = longOrNull("lastWateredAt"),
        waterCountToday = optInt("waterCountToday", 0),
        waterCountDayEpoch = optLong("waterCountDayEpoch", 0),
        lastFertilizedAt = longOrNull("lastFertilizedAt"),
        orientationOverride = stringOrNull("orientationOverride")?.let { Orientation.valueOf(it) },
        sunHoursOverride = floatOrNull("sunHoursOverride"),
        identifiedSpecies = stringOrNull("identifiedSpecies"),
        aiWateringIntervalDays = intOrNull("aiWateringIntervalDays"),
        soilMoistureLevel = intOrNull("soilMoistureLevel"),
        soilMoistureReadingAt = longOrNull("soilMoistureReadingAt")
    )

    private fun JSONObject.toPlantPhotoEntity(newPlantId: Long, filePath: String) = PlantPhotoEntity(
        plantId = newPlantId,
        filePath = filePath,
        takenAt = getLong("takenAt"),
        analysisStatus = runCatching { AnalysisStatus.valueOf(getString("analysisStatus")) }.getOrDefault(AnalysisStatus.DONE),
        aiSuggestedHealth = stringOrNull("aiSuggestedHealth")?.let { HealthState.valueOf(it) },
        confirmedHealth = stringOrNull("confirmedHealth")?.let { HealthState.valueOf(it) },
        diagnosis = stringOrNull("diagnosis"),
        wateringTip = stringOrNull("wateringTip"),
        pruningTip = stringOrNull("pruningTip"),
        fertilizingTip = stringOrNull("fertilizingTip"),
        repottingTip = stringOrNull("repottingTip"),
        identifiedSpecies = stringOrNull("identifiedSpecies"),
        recommendedWateringIntervalDays = intOrNull("recommendedWateringIntervalDays"),
        evolutionNote = stringOrNull("evolutionNote")
    )

    private fun photoEntryName(context: Context, photo: PlantPhotoEntity): String {
        val root = File(context.filesDir, "plant_photos")
        return File(photo.filePath).relativeToOrSelf(root).path
    }

    private fun PlantEntity.toJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("category", category.name)
        put("customIntervalDays", customIntervalDays ?: JSONObject.NULL)
        put("notes", notes)
        put("createdAt", createdAt)
        put("lastWateredAt", lastWateredAt ?: JSONObject.NULL)
        put("waterCountToday", waterCountToday)
        put("waterCountDayEpoch", waterCountDayEpoch)
        put("lastFertilizedAt", lastFertilizedAt ?: JSONObject.NULL)
        put("orientationOverride", orientationOverride?.name ?: JSONObject.NULL)
        put("sunHoursOverride", sunHoursOverride ?: JSONObject.NULL)
        put("identifiedSpecies", identifiedSpecies ?: JSONObject.NULL)
        put("aiWateringIntervalDays", aiWateringIntervalDays ?: JSONObject.NULL)
        put("soilMoistureLevel", soilMoistureLevel ?: JSONObject.NULL)
        put("soilMoistureReadingAt", soilMoistureReadingAt ?: JSONObject.NULL)
    }

    private fun PlantPhotoEntity.toJson(context: Context) = JSONObject().apply {
        put("id", id)
        put("plantId", plantId)
        put("relativePath", photoEntryName(context, this@toJson))
        put("takenAt", takenAt)
        put("analysisStatus", analysisStatus.name)
        put("aiSuggestedHealth", aiSuggestedHealth?.name ?: JSONObject.NULL)
        put("confirmedHealth", confirmedHealth?.name ?: JSONObject.NULL)
        put("diagnosis", diagnosis ?: JSONObject.NULL)
        put("wateringTip", wateringTip ?: JSONObject.NULL)
        put("pruningTip", pruningTip ?: JSONObject.NULL)
        put("fertilizingTip", fertilizingTip ?: JSONObject.NULL)
        put("repottingTip", repottingTip ?: JSONObject.NULL)
        put("identifiedSpecies", identifiedSpecies ?: JSONObject.NULL)
        put("recommendedWateringIntervalDays", recommendedWateringIntervalDays ?: JSONObject.NULL)
        put("evolutionNote", evolutionNote ?: JSONObject.NULL)
    }

    private fun WateringLogEntity.toJson() = JSONObject().apply {
        put("id", id)
        put("plantId", plantId)
        put("wateredAt", wateredAt)
    }

    private fun ChatMessageEntity.toJson() = JSONObject().apply {
        put("id", id)
        put("plantId", plantId)
        put("role", role.name)
        put("content", content)
        put("createdAt", createdAt)
    }
}
