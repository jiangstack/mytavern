package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PromptBlockType(val displayName: String, val editable: Boolean) {
    SYSTEM_ROLE("系统角色", true),
    NOVEL_META("小说元信息", false),
    WORLD_BOOK("世界书信息", false),
    CHARACTERS("角色信息", false),
    CHAPTER_OUTLINES("章节纲要", false),
    PREVIOUS_CHAPTER("上一章正文", false),
    CURRENT_CHAPTER("当前章节信息", false),
    EXISTING_CONTENT("已有正文", false),
    SELECTED_TEXT("待修改文本", false),
    CHAPTER_CONTENT("章节正文", false),
    CUSTOM_REQUEST("用户附加要求", true),
    OUTPUT_INSTRUCTION("输出要求", true)
}
