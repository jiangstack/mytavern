package org.jiangstack.mytavern.domain.service

import org.jiangstack.mytavern.data.remote.ToolCall
import org.jiangstack.mytavern.data.remote.Usage

data class StreamChunk(
    val content: String = "",
    val reasoningContent: String = "",
    val usage: Usage? = null,
    val toolCalls: List<ToolCall>? = null
)
