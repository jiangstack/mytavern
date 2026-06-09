package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PromptBlockConfig(
    val type: PromptBlockType,
    val isEnabled: Boolean = true,
    val sortOrder: Int,
    val customContent: String? = null
)
