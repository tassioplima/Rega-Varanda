package com.tassiolima.regavaranda.data.repository

import com.tassiolima.regavaranda.data.local.PlantPhotoDao
import com.tassiolima.regavaranda.data.local.PlantPhotoEntity
import kotlinx.coroutines.flow.Flow

class PlantPhotoRepository(private val dao: PlantPhotoDao) {

    fun observeForPlant(plantId: Long): Flow<List<PlantPhotoEntity>> = dao.observeForPlant(plantId)

    fun observeAll(): Flow<List<PlantPhotoEntity>> = dao.observeAll()

    suspend fun getById(id: Long): PlantPhotoEntity? = dao.getById(id)

    suspend fun insert(photo: PlantPhotoEntity): Long = dao.insert(photo)

    suspend fun update(photo: PlantPhotoEntity) = dao.update(photo)

    suspend fun delete(photo: PlantPhotoEntity) = dao.delete(photo)
}
