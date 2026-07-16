package com.tassiolima.regavaranda.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun getById(id: Long): PlantEntity?

    @Query("SELECT * FROM plants WHERE id = :id")
    fun observeById(id: Long): Flow<PlantEntity?>

    @Insert
    suspend fun insert(plant: PlantEntity): Long

    @Update
    suspend fun update(plant: PlantEntity)

    @Delete
    suspend fun delete(plant: PlantEntity)
}
