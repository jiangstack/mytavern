package org.jiangstack.mytavern.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import org.jiangstack.mytavern.data.local.dao.CharacterDao
import org.jiangstack.mytavern.data.local.dao.ChatMessageDao
import org.jiangstack.mytavern.data.local.dao.ChatSessionDao
import org.jiangstack.mytavern.data.local.dao.InteractiveGameCharacterDao
import org.jiangstack.mytavern.data.local.dao.InteractiveGameDao
import org.jiangstack.mytavern.data.local.dao.InteractiveGameStateDao
import org.jiangstack.mytavern.data.local.dao.InteractiveMessageDao
import org.jiangstack.mytavern.data.local.dao.LlmConfigDao
import org.jiangstack.mytavern.data.local.dao.NovelCharacterDao
import org.jiangstack.mytavern.data.local.dao.NovelCharacterItemDao
import org.jiangstack.mytavern.data.local.dao.NovelChapterDao
import org.jiangstack.mytavern.data.local.dao.NovelDao
import org.jiangstack.mytavern.data.local.dao.QuickReplyDao
import org.jiangstack.mytavern.data.local.dao.SessionCharacterDao
import org.jiangstack.mytavern.data.local.dao.SessionStateDao
import org.jiangstack.mytavern.data.local.dao.WorldBookDao
import org.jiangstack.mytavern.data.local.dao.WorldBookRuleDao
import org.jiangstack.mytavern.data.local.entity.CharacterEntity
import org.jiangstack.mytavern.data.local.entity.ChatMessageEntity
import org.jiangstack.mytavern.data.local.entity.ChatSessionEntity
import org.jiangstack.mytavern.data.local.entity.InteractiveGameCharacterEntity
import org.jiangstack.mytavern.data.local.entity.InteractiveGameEntity
import org.jiangstack.mytavern.data.local.entity.InteractiveGameStateEntity
import org.jiangstack.mytavern.data.local.entity.InteractiveMessageEntity
import org.jiangstack.mytavern.data.local.entity.LlmConfigEntity
import org.jiangstack.mytavern.data.local.entity.NovelChapterEntity
import org.jiangstack.mytavern.data.local.entity.NovelCharacterEntity
import org.jiangstack.mytavern.data.local.entity.NovelCharacterItemEntity
import org.jiangstack.mytavern.data.local.entity.NovelEntity
import org.jiangstack.mytavern.data.local.entity.QuickReplyEntity
import org.jiangstack.mytavern.data.local.entity.SessionCharacterEntity
import org.jiangstack.mytavern.data.local.entity.SessionStateEntity
import org.jiangstack.mytavern.data.local.entity.WorldBookEntity
import org.jiangstack.mytavern.data.local.entity.WorldBookRuleEntity

@Database(
    entities = [
        CharacterEntity::class,
        WorldBookEntity::class,
        WorldBookRuleEntity::class,
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        LlmConfigEntity::class,
        SessionCharacterEntity::class,
        SessionStateEntity::class,
        QuickReplyEntity::class,
        NovelEntity::class,
        NovelChapterEntity::class,
        NovelCharacterEntity::class,
        NovelCharacterItemEntity::class,
        InteractiveGameEntity::class,
        InteractiveGameCharacterEntity::class,
        InteractiveMessageEntity::class,
        InteractiveGameStateEntity::class
    ],
    version = 13,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun worldBookDao(): WorldBookDao
    abstract fun worldBookRuleDao(): WorldBookRuleDao
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun llmConfigDao(): LlmConfigDao
    abstract fun sessionCharacterDao(): SessionCharacterDao
    abstract fun sessionStateDao(): SessionStateDao
    abstract fun quickReplyDao(): QuickReplyDao
    abstract fun novelDao(): NovelDao
    abstract fun novelChapterDao(): NovelChapterDao
    abstract fun novelCharacterDao(): NovelCharacterDao
    abstract fun novelCharacterItemDao(): NovelCharacterItemDao
    abstract fun interactiveGameDao(): InteractiveGameDao
    abstract fun interactiveGameCharacterDao(): InteractiveGameCharacterDao
    abstract fun interactiveMessageDao(): InteractiveMessageDao
    abstract fun interactiveGameStateDao(): InteractiveGameStateDao
}
