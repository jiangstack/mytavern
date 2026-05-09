package org.jiangstack.mytavern.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.jiangstack.mytavern.data.local.dao.CharacterDao
import org.jiangstack.mytavern.data.local.dao.ChatMessageDao
import org.jiangstack.mytavern.data.local.dao.ChatSessionDao
import org.jiangstack.mytavern.data.local.dao.LlmConfigDao
import org.jiangstack.mytavern.data.local.dao.WorldBookDao
import org.jiangstack.mytavern.data.local.dao.WorldBookRuleDao
import org.jiangstack.mytavern.data.local.entity.CharacterEntity
import org.jiangstack.mytavern.data.local.entity.ChatMessageEntity
import org.jiangstack.mytavern.data.local.entity.ChatSessionEntity
import org.jiangstack.mytavern.data.local.entity.LlmConfigEntity
import org.jiangstack.mytavern.data.local.entity.WorldBookEntity
import org.jiangstack.mytavern.data.local.entity.WorldBookRuleEntity

@Database(
    entities = [
        CharacterEntity::class,
        WorldBookEntity::class,
        WorldBookRuleEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        LlmConfigEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun worldBookDao(): WorldBookDao
    abstract fun worldBookRuleDao(): WorldBookRuleDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun llmConfigDao(): LlmConfigDao
}
