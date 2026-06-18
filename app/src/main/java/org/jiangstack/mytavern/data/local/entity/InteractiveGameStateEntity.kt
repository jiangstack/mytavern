package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interactive_game_states",
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
data class InteractiveGameStateEntity(
    @PrimaryKey
    val gameId: Long,
    val environment: String = "",
    val characterStatus: String = "",
    val characterItems: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)
