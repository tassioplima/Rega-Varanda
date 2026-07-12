package com.tassiolima.regavaranda.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watering_log")
data class WateringLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val plantId: Long,
    val wateredAt: Long
)
