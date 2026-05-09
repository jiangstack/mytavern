package org.jiangstack.mytavern.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llm_configs")
data class LlmConfigEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val apiType: String,
    val baseUrl: String,
    val apiKey: String,
    val model: String
)
