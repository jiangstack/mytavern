package org.jiangstack.mytavern.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class PromptBlockType(val displayName: String, val editable: Boolean) {
    SYSTEM_ROLE("系统角色", true),
    NOVEL_META("小说元信息", false),
    WORLD_BOOK("世界书信息", false),
    CHARACTERS("角色信息", false),
    CHARACTER_ITEMS("人物物品", false),
    CHAPTER_OUTLINES("章节纲要", false),
    PREVIOUS_CHAPTER("上一章正文", false),
    CURRENT_CHAPTER("当前章节信息", false),
    EXISTING_CONTENT("已有正文", false),
    SELECTED_TEXT("待修改文本", false),
    CHAPTER_CONTENT("章节正文", false),
    CUSTOM_REQUEST("用户附加要求", true),
    OUTPUT_INSTRUCTION("输出要求", true),
    INTERACTIVE_SYSTEM_ROLE("系统角色", true),
    INTERACTIVE_NARRATOR_STYLE("讲述者风格", false),
    INTERACTIVE_PARTICIPATING_CHARACTERS("参与角色", false),
    INTERACTIVE_PLAY_CHARACTER("扮演角色", false),
    INTERACTIVE_STORY_BACKGROUND("故事背景", false),
    INTERACTIVE_STORY_MAIN_PLOT("故事主线", false),
    INTERACTIVE_WORLD_BOOK("世界书", false),
    INTERACTIVE_STORY_CONTENT("故事内容", false),
    INTERACTIVE_CURRENT_STATE("当前状态", false),
    INTERACTIVE_USER_CHOICE("用户选择", false),
    INTERACTIVE_OUTPUT_INSTRUCTION("输出要求", true),
    TOWN_SYSTEM_ROLE("小镇系统角色", true),
    TOWN_WORLD_SETTING("小镇设定", false),
    TOWN_CHARACTERS("小镇角色", false),
    TOWN_CURRENT_STATE("小镇当前状态", false),
    TOWN_RECENT_LOGS("小镇日志", false),
    TOWN_OUTPUT_INSTRUCTION("小镇输出要求", true)
}
