package com.tassiolima.regavaranda.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantPhotoDao {
    @Query("SELECT * FROM plant_photos WHERE plantId = :plantId ORDER BY takenAt DESC")
    fun observeForPlant(plantId: Long): Flow<List<PlantPhotoEntity>>

    @Query("SELECT * FROM plant_photos ORDER BY takenAt DESC")
    fun observeAll(): Flow<List<PlantPhotoEntity>>

    @Query("SELECT * FROM plant_photos WHERE id = :id")
    suspend fun getById(id: Long): PlantPhotoEntity?

    @Insert
    suspend fun insert(photo: PlantPhotoEntity): Long

    @Update
    suspend fun update(photo: PlantPhotoEntity)

    @Delete
    suspend fun delete(photo: PlantPhotoEntity)
}
