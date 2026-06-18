package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS novel_character_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                novelId INTEGER NOT NULL,
                characterId INTEGER NOT NULL,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY(novelId) REFERENCES novels(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS index_novel_character_items_novelId_characterId
            ON novel_character_items(novelId, characterId)
            """.trimIndent()
        )
    }
}
