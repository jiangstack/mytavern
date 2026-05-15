package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE session_characters (
                sessionId INTEGER NOT NULL,
                characterId INTEGER NOT NULL,
                PRIMARY KEY(sessionId, characterId),
                FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX index_session_characters_sessionId ON session_characters(sessionId)")
    }
}
