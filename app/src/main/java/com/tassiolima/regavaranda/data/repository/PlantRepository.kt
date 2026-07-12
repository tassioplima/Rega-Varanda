package com.tassiolima.regavaranda.data.repository

import com.tassiolima.regavaranda.data.local.PlantDao
import com.tassiolima.regavaranda.data.local.PlantEntity
import com.tassiolima.regavaranda.data.local.WateringLogDao
import com.tassiolima.regavaranda.data.local.WateringLogEntity
import com.tassiolima.regavaranda.util.DateUtils
import kotlinx.coroutines.flow.Flow

class PlantRepository(
    private val dao: PlantDao,
    private val wateringLogDao: WateringLogDao
) {

    fun observePlants(): Flow<List<PlantEntity>> = dao.observeAll()

    suspend fun getPlant(id: Long): PlantEntity? = dao.getById(id)

    suspend fun savePlant(plant: PlantEntity) {
        if (plant.id == 0L) dao.insert(plant) else dao.update(plant)
    }

    suspend fun deletePlant(plant: PlantEntity) = dao.delete(plant)

    fun observeWateringLog(): Flow<List<WateringLogEntity>> = wateringLogDao.observeAll()

    suspend fun getWateringLog(plantId: Long): List<WateringLogEntity> = wateringLogDao.getForPlant(plantId)

    suspend fun markWatered(plant: PlantEntity, nowMillis: Long, todayEpochDay: Long) {
        val isSameDay = plant.waterCountDayEpoch == todayEpochDay
        val updated = plant.copy(
            lastWateredAt = nowMillis,
            waterCountToday = if (isSameDay) plant.waterCountToday + 1 else 1,
            waterCountDayEpoch = todayEpochDay
        )
        dao.update(updated)
        wateringLogDao.insert(WateringLogEntity(plantId = plant.id, wateredAt = nowMillis))
    }

    /** Desfaz a última rega registrada (útil quando o usuário toca "Reguei agora" sem querer). */
    suspend fun undoLastWatering(plant: PlantEntity, todayEpochDay: Long) {
        val latest = wateringLogDao.getLatest(plant.id) ?: return
        wateringLogDao.delete(latest)

        val remaining = wateringLogDao.getForPlant(plant.id)
        val newLast = remaining.firstOrNull()
        val newCountToday = remaining.count { DateUtils.epochDayOf(it.wateredAt) == todayEpochDay }

        dao.update(
            plant.copy(
                lastWateredAt = newLast?.wateredAt,
                waterCountToday = newCountToday,
                waterCountDayEpoch = todayEpochDay
            )
        )
    }

    suspend fun markFertilized(plant: PlantEntity, nowMillis: Long) {
        dao.update(plant.copy(lastFertilizedAt = nowMillis))
    }

    suspend fun recordMoistureReading(plant: PlantEntity, level: Int, nowMillis: Long) {
        dao.update(plant.copy(soilMoistureLevel = level, soilMoistureReadingAt = nowMillis))
    }
}
