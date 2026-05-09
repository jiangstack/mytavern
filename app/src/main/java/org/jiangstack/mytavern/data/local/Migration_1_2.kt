package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE characters ADD COLUMN type TEXT NOT NULL DEFAULT 'AI'")
        db.execSQL("ALTER TABLE chat_sessions ADD COLUMN userCharacterId INTEGER")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN senderName TEXT")
    }
}
