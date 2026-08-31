package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "town_relationships",
    foreignKeys = [
        ForeignKey(
            entity = TownEntity::class,
            parentColumns = ["id"],
            childColumns = ["townId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["townId"]),
        Index(value = ["townId", "memberAId", "memberBId"], unique = true)
    ]
)
data class TownRelationshipEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val townId: Long,
    val memberAId: Long,
    val memberBId: Long,
    val affinity: Int = 0,
    val note: String = ""
)
