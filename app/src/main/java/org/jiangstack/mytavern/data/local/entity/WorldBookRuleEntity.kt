package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "world_book_rules",
    foreignKeys = [
        ForeignKey(
            entity = WorldBookEntity::class,
            parentColumns = ["id"],
            childColumns = ["worldBookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["worldBookId"])]
)
data class WorldBookRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val worldBookId: Long,
    val name: String,
    val description: String,
    val matchType: String
)
