package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS interactive_games (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                narratorStyle TEXT NOT NULL DEFAULT '',
                storyBackground TEXT NOT NULL DEFAULT '',
                storyMainPlot TEXT NOT NULL DEFAULT '',
                windowWordCount INTEGER NOT NULL DEFAULT 3000,
                playCharacterId INTEGER NOT NULL,
                worldBookId INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS interactive_game_characters (
                gameId INTEGER NOT NULL,
                characterId INTEGER NOT NULL,
                PRIMARY KEY(gameId, characterId),
                FOREIGN KEY(gameId) REFERENCES interactive_games(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_interactive_game_characters_gameId
            ON interactive_game_characters(gameId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS interactive_messages (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                gameId INTEGER NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                actionOptions TEXT,
                timestamp INTEGER NOT NULL,
                FOREIGN KEY(gameId) REFERENCES interactive_games(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_interactive_messages_gameId
            ON interactive_messages(gameId)
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS interactive_game_states (
                gameId INTEGER PRIMARY KEY NOT NULL,
                environment TEXT NOT NULL DEFAULT '',
                characterStatus TEXT NOT NULL DEFAULT '',
                characterItems TEXT NOT NULL DEFAULT '',
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(gameId) REFERENCES interactive_games(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_interactive_game_states_gameId
            ON interactive_game_states(gameId)
            """.trimIndent()
        )
    }
}
