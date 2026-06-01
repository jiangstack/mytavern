package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE novels (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL DEFAULT '',
                worldBookId INTEGER,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE novel_chapters (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                novelId INTEGER NOT NULL,
                chapterNumber INTEGER NOT NULL,
                title TEXT NOT NULL,
                outline TEXT NOT NULL DEFAULT '',
                content TEXT NOT NULL DEFAULT '',
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL,
                FOREIGN KEY (novelId) REFERENCES novels(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX index_novel_chapters_novelId ON novel_chapters(novelId)")

        db.execSQL(
            """
            CREATE TABLE novel_characters (
                novelId INTEGER NOT NULL,
                characterId INTEGER NOT NULL,
                PRIMARY KEY(novelId, characterId),
                FOREIGN KEY (novelId) REFERENCES novels(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX index_novel_characters_novelId ON novel_characters(novelId)")
    }
}
