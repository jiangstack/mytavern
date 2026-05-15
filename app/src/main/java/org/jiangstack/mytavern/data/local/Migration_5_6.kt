package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE session_states (
                sessionId INTEGER NOT NULL,
                stateKey TEXT NOT NULL,
                stateValue TEXT NOT NULL,
                updatedAt INTEGER NOT NULL,
                PRIMARY KEY(sessionId, stateKey),
                FOREIGN KEY(sessionId) REFERENCES chat_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("ALTER TABLE chat_sessions ADD COLUMN sessionStateEnabled INTEGER NOT NULL DEFAULT 0")
    }
}
