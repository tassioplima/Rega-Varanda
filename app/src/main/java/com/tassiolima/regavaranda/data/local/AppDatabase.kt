package com.tassiolima.regavaranda.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [PlantEntity::class, PlantPhotoEntity::class, ChatMessageEntity::class, WateringLogEntity::class],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun plantPhotoDao(): PlantPhotoDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun wateringLogDao(): WateringLogDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rega_varanda.db"
                ).addMigrations(*ALL_MIGRATIONS).build().also { instance = it }
            }
    }
}
