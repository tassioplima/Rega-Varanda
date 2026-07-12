package com.tassiolima.regavaranda.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WateringLogDao {
    @Query("SELECT * FROM watering_log ORDER BY wateredAt DESC")
    fun observeAll(): Flow<List<WateringLogEntity>>

    @Query("SELECT * FROM watering_log WHERE plantId = :plantId ORDER BY wateredAt DESC")
    suspend fun getForPlant(plantId: Long): List<WateringLogEntity>

    @Query("SELECT * FROM watering_log WHERE plantId = :plantId ORDER BY wateredAt DESC LIMIT 1")
    suspend fun getLatest(plantId: Long): WateringLogEntity?

    @Insert
    suspend fun insert(entry: WateringLogEntity): Long

    @Delete
    suspend fun delete(entry: WateringLogEntity)
}
