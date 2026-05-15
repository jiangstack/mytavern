package org.jiangstack.mytavern.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN promptTokens INTEGER")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN completionTokens INTEGER")
        db.execSQL("ALTER TABLE chat_messages ADD COLUMN totalTokens INTEGER")
    }
}
