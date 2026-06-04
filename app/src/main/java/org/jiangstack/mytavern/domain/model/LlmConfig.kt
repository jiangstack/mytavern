package org.jiangstack.mytavern.domain.model

data class LlmConfig(
    val id: Long = 0,
    val name: String,
    val apiType: ApiType,
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val customParams: String? = null
)

enum class ApiType {
    OPENAI,
    OPENRESPONSES,
    ANTHROPIC
}
