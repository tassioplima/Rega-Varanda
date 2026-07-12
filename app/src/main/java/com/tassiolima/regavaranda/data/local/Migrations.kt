package com.tassiolima.regavaranda.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrações reais do banco, para que os dados do usuário nunca sejam apagados numa
 * atualização do app. A versão atual do schema é 7 (ver [AppDatabase]).
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

val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    MIGRATION_5_6,
    MIGRATION_6_7
    // Adicione as próximas migrações (7 -> 8, 8 -> 9, ...) aqui.
)
