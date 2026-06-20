package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS interactive_checkpoints (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                gameId INTEGER NOT NULL,
                parentId INTEGER,
                name TEXT NOT NULL,
                snapshot TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(gameId) REFERENCES interactive_games(id) ON DELETE CASCADE,
                FOREIGN KEY(parentId) REFERENCES interactive_checkpoints(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_interactive_checkpoints_gameId
            ON interactive_checkpoints(gameId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_interactive_checkpoints_parentId
            ON interactive_checkpoints(parentId)
            """.trimIndent()
        )

        db.execSQL(
            "ALTER TABLE interactive_game_states ADD COLUMN activeCheckpointId INTEGER"
        )
    }
}
