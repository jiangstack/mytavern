package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS image_api_configs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                baseUrl TEXT NOT NULL,
                apiKey TEXT NOT NULL,
                model TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "ALTER TABLE interactive_games ADD COLUMN backgroundImageUri TEXT"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS interactive_game_images (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                gameId INTEGER NOT NULL,
                remoteUrl TEXT NOT NULL,
                localUri TEXT,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(gameId) REFERENCES interactive_games(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_interactive_game_images_gameId
            ON interactive_game_images(gameId)
            """.trimIndent()
        )
    }
}
