package com.tassiolima.regavaranda.util

import android.content.Context
import com.tassiolima.regavaranda.data.local.ChatMessageEntity
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.PlantPhotoEntity
import com.tassiolima.regavaranda.data.local.WateringLogEntity
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
import java.util.zip.ZipOutputStream

/** Exporta plantas, fotos, histórico de rega e chats para um único arquivo .zip compartilhável. */
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
