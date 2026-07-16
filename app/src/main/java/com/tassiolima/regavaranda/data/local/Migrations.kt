package com.tassiolima.regavaranda.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrações reais do banco, para que os dados do usuário nunca sejam apagados numa
 * atualização do app. A versão atual do schema é 8 (ver [AppDatabase]).
 *
 * Ao adicionar/alterar uma coluna ou tabela:
 *   1. Mude a entidade (ex.: PlantEntity).
 *   2. Suba `version` em [AppDatabase].
 *   3. Escreva uma nova `Migration(N, N+1)` aqui com o SQL equivalente à mudança
 *      (ex.: "ALTER TABLE plants ADD COLUMN novoCampo TEXT") e adicione-a em [ALL_MIGRATIONS].
 *
 * Nunca use `fallbackToDestructiveMigration()` — isso apaga todos os dados do usuário
 * sempre que o schema muda.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plant_photos ADD COLUMN evolutionNote TEXT")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watering_log` (
                `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                `plantId` INTEGER NOT NULL,
                `wateredAt` INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plants ADD COLUMN soilMoistureLevel INTEGER")
        db.execSQL("ALTER TABLE plants ADD COLUMN soilMoistureReadingAt INTEGER")
    }
}

/**
 * Adiciona FOREIGN KEY com ON DELETE CASCADE (+ índice em plantId) a plant_photos,
 * chat_messages e watering_log, para que apagar uma planta remova junto todo o histórico
 * dela. SQLite não permite adicionar FK a tabela existente, então cada tabela é recriada
 * e os dados copiados; linhas órfãs (de plantas já apagadas antes desta versão) são
 * descartadas na cópia.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `plant_photos_new` (
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
                `evolutionNote` TEXT,
                FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO plant_photos_new
            SELECT * FROM plant_photos WHERE plantId IN (SELECT id FROM plants)
            """.trimIndent()
        )
        db.execSQL("DROP TABLE plant_photos")
        db.execSQL("ALTER TABLE plant_photos_new RENAME TO plant_photos")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_plant_photos_plantId` ON `plant_photos` (`plantId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_messages_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `plantId` INTEGER NOT NULL,
                `role` TEXT NOT NULL,
                `content` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO chat_messages_new
            SELECT * FROM chat_messages WHERE plantId IN (SELECT id FROM plants)
            """.trimIndent()
        )
        db.execSQL("DROP TABLE chat_messages")
        db.execSQL("ALTER TABLE chat_messages_new RENAME TO chat_messages")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_chat_messages_plantId` ON `chat_messages` (`plantId`)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `watering_log_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `plantId` INTEGER NOT NULL,
                `wateredAt` INTEGER NOT NULL,
                FOREIGN KEY(`plantId`) REFERENCES `plants`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO watering_log_new
            SELECT * FROM watering_log WHERE plantId IN (SELECT id FROM plants)
            """.trimIndent()
        )
        db.execSQL("DROP TABLE watering_log")
        db.execSQL("ALTER TABLE watering_log_new RENAME TO watering_log")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_watering_log_plantId` ON `watering_log` (`plantId`)")
    }
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_5_6,
    MIGRATION_6_7,
    MIGRATION_7_8
    // Adicione as próximas migrações (8 -> 9, 9 -> 10, ...) aqui.
)
