package com.tassiolima.regavaranda.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test.db"

/**
 * Garante que a migração 7 -> 8 preserva os dados do usuário (requisito número 1 do app),
 * descarta linhas órfãs e deixa o CASCADE funcionando. O banco v7 é criado na mão com o
 * SQL da época, já que os schemas exportados só existem a partir da v8.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun migracao7para8_preservaDados_descartaOrfaos_eAtivaCascade() {
        context.deleteDatabase(TEST_DB)
        criaBancoV7ComDados()

        // Abrir com Room roda MIGRATION_7_8 e valida o schema resultante contra as
        // entidades da v8 — se a migração gerar schema diferente, esta linha lança.
        val db = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(*ALL_MIGRATIONS)
            .build()

        try {
            runBlocking {
                val plants = db.plantDao().getById(1)
                assertEquals("Zamioculca", plants?.name)

                // Dados da planta 1 preservados; órfãos (plantId=99) descartados.
                val photos = db.plantPhotoDao().getById(1)
                assertEquals("/data/foto1.jpg", photos?.filePath)
                assertEquals(1, db.chatMessageDao().getAll().size)
                assertEquals(1, db.wateringLogDao().getForPlant(1).size)
                assertEquals(0, db.wateringLogDao().getForPlant(99).size)

                // CASCADE: apagar a planta remove fotos, chat e histórico juntos.
                db.plantDao().delete(db.plantDao().getById(1)!!)
                assertTrue(db.chatMessageDao().getAll().isEmpty())
                assertTrue(db.wateringLogDao().getForPlant(1).isEmpty())
                assertEquals(null, db.plantPhotoDao().getById(1))
            }
        } finally {
            db.close()
            context.deleteDatabase(TEST_DB)
        }
    }

    /** Cria o banco exatamente como a versão 7 o deixava (sem FKs, sem índices). */
    private fun criaBancoV7ComDados() {
        val dbFile = context.getDatabasePath(TEST_DB).apply { parentFile?.mkdirs() }
        val db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        db.use {
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `plants` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `customIntervalDays` INTEGER,
                    `notes` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `lastWateredAt` INTEGER,
                    `waterCountToday` INTEGER NOT NULL,
                    `waterCountDayEpoch` INTEGER NOT NULL,
                    `lastFertilizedAt` INTEGER,
                    `orientationOverride` TEXT,
                    `sunHoursOverride` REAL,
                    `identifiedSpecies` TEXT,
                    `aiWateringIntervalDays` INTEGER,
                    `soilMoistureLevel` INTEGER,
                    `soilMoistureReadingAt` INTEGER
                )
                """.trimIndent()
            )
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `plant_photos` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `plantId` INTEGER NOT NULL,
                    `filePath` TEXT NOT NULL,
                    `takenAt` INTEGER NOT NULL,
                    `analysisStatus` TEXT NOT NULL,
                    `aiSuggestedHealth` TEXT,
                    `confirmedHealth` TEXT,
                    `diagnosis` TEXT,
                    `wateringTip` TEXT,
                    `pruningTip` TEXT,
                    `fertilizingTip` TEXT,
                    `repottingTip` TEXT,
                    `identifiedSpecies` TEXT,
                    `recommendedWateringIntervalDays` INTEGER,
                    `evolutionNote` TEXT
                )
                """.trimIndent()
            )
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `chat_messages` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `plantId` INTEGER NOT NULL,
                    `role` TEXT NOT NULL,
                    `content` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            it.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `watering_log` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `plantId` INTEGER NOT NULL,
                    `wateredAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )

            it.execSQL(
                """
                INSERT INTO plants (id, name, category, notes, createdAt, waterCountToday, waterCountDayEpoch)
                VALUES (1, 'Zamioculca', 'OUTRA', '', 1000, 0, 0)
                """.trimIndent()
            )
            it.execSQL("INSERT INTO plant_photos (id, plantId, filePath, takenAt, analysisStatus) VALUES (1, 1, '/data/foto1.jpg', 2000, 'DONE')")
            it.execSQL("INSERT INTO plant_photos (id, plantId, filePath, takenAt, analysisStatus) VALUES (2, 99, '/data/orfa.jpg', 2000, 'DONE')")
            it.execSQL("INSERT INTO chat_messages (plantId, role, content, createdAt) VALUES (1, 'USER', 'oi', 3000)")
            it.execSQL("INSERT INTO chat_messages (plantId, role, content, createdAt) VALUES (99, 'USER', 'orfa', 3000)")
            it.execSQL("INSERT INTO watering_log (plantId, wateredAt) VALUES (1, 4000)")
            it.execSQL("INSERT INTO watering_log (plantId, wateredAt) VALUES (99, 4000)")
            it.version = 7
        }
    }
}
