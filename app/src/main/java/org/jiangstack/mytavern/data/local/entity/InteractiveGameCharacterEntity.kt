package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "interactive_game_characters",
    primaryKeys = ["gameId", "characterId"],
    foreignKeys = [
        ForeignKey(
            entity = InteractiveGameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["gameId"])]
)
data class InteractiveGameCharacterEntity(
    val gameId: Long,
    val characterId: Long
)
