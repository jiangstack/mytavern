package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_sessions ADD COLUMN novelId INTEGER")
        db.execSQL("ALTER TABLE chat_sessions ADD COLUMN agentSystemPrompt TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN role TEXT")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN messageType TEXT")
    }
}
