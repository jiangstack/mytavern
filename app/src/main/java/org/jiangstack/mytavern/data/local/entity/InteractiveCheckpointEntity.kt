package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interactive_checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = InteractiveGameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = InteractiveCheckpointEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["gameId"]),
        Index(value = ["parentId"])
    ]
)
data class InteractiveCheckpointEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: Long,
    val parentId: Long? = null,
    val name: String,
    val snapshot: String,
    val createdAt: Long = System.currentTimeMillis()
)
