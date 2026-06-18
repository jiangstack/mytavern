package org.jiangstack.mytavern.domain.model

object PromptBlockDefaults {

    fun defaultContent(
        type: PromptBlockType,
        isContinue: Boolean = true,
        isOutline: Boolean = false
    ): String? {
        if (isOutline) {
            return when (type) {
                PromptBlockType.SYSTEM_ROLE ->
                    "你是一位小说编辑助手。请根据以下章节正文，总结出简洁的章节纲要。"

                PromptBlockType.OUTPUT_INSTRUCTION ->
                    "请用简洁的语言总结本章的主要情节、关键事件和转折点。直接输出纲要内容，不要加任何标题或解释。控制在200字以内。"

                else -> null
            }
        }
        return when (type) {
            PromptBlockType.SYSTEM_ROLE ->
                if (isContinue) "你是一位小说创作助手，请根据以下信息续写小说。"
                else "你是一位小说编辑助手，请根据用户的修改要求对指定文本进行修改。"

            PromptBlockType.CUSTOM_REQUEST ->
                if (isContinue) "用户附加要求：{customRequest}"
                else "用户修改要求：{customRequest}"

            PromptBlockType.OUTPUT_INSTRUCTION ->
                if (isContinue) "请续写小说正文，保持风格一致，承接前文情节。直接输出续写内容，不要重复前文，不要加任何解释或标题。"
                else "请根据修改要求对上述文本进行修改。保持与原文风格一致，保持上下文连贯。直接输出修改后的文本，不要加任何解释、标题或标记。"

            PromptBlockType.INTERACTIVE_SYSTEM_ROLE ->
                "你是一个互动故事的讲述者。"

            PromptBlockType.INTERACTIVE_OUTPUT_INSTRUCTION ->
                "请继续推进故事。使用 function call 更新人物状态、人物物品、当前环境，并提供3-5个行动选项。故事内容直接输出，不要加标题或解释。"

            else -> null
        }
    }

    fun continueWritingBlocks(): List<PromptBlockConfig> = listOf(
        PromptBlockConfig(PromptBlockType.SYSTEM_ROLE, true, 0),
        PromptBlockConfig(PromptBlockType.NOVEL_META, true, 1),
        PromptBlockConfig(PromptBlockType.WORLD_BOOK, true, 2),
        PromptBlockConfig(PromptBlockType.CHARACTERS, true, 3),
        PromptBlockConfig(PromptBlockType.CHARACTER_ITEMS, true, 4),
        PromptBlockConfig(PromptBlockType.CHAPTER_OUTLINES, true, 5),
        PromptBlockConfig(PromptBlockType.PREVIOUS_CHAPTER, true, 6),
        PromptBlockConfig(PromptBlockType.CURRENT_CHAPTER, true, 7),
        PromptBlockConfig(PromptBlockType.EXISTING_CONTENT, true, 8),
        PromptBlockConfig(PromptBlockType.CUSTOM_REQUEST, true, 9),
        PromptBlockConfig(PromptBlockType.OUTPUT_INSTRUCTION, true, 10)
    )

    fun modifyBlocks(): List<PromptBlockConfig> = listOf(
        PromptBlockConfig(PromptBlockType.SYSTEM_ROLE, true, 0),
        PromptBlockConfig(PromptBlockType.NOVEL_META, true, 1),
        PromptBlockConfig(PromptBlockType.WORLD_BOOK, true, 2),
        PromptBlockConfig(PromptBlockType.CHARACTERS, true, 3),
        PromptBlockConfig(PromptBlockType.CHARACTER_ITEMS, true, 4),
        PromptBlockConfig(PromptBlockType.CHAPTER_OUTLINES, true, 5),
        PromptBlockConfig(PromptBlockType.CURRENT_CHAPTER, true, 6),
        PromptBlockConfig(PromptBlockType.SELECTED_TEXT, true, 7),
        PromptBlockConfig(PromptBlockType.OUTPUT_INSTRUCTION, true, 8)
    )

    fun outlineBlocks(): List<PromptBlockConfig> = listOf(
        PromptBlockConfig(PromptBlockType.SYSTEM_ROLE, true, 0),
        PromptBlockConfig(PromptBlockType.CURRENT_CHAPTER, true, 1),
        PromptBlockConfig(PromptBlockType.CHAPTER_CONTENT, true, 2),
        PromptBlockConfig(PromptBlockType.OUTPUT_INSTRUCTION, true, 3)
    )

    fun interactiveStoryBlocks(): List<PromptBlockConfig> = listOf(
        PromptBlockConfig(PromptBlockType.INTERACTIVE_SYSTEM_ROLE, true, 0),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_NARRATOR_STYLE, true, 1),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_PARTICIPATING_CHARACTERS, true, 2),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_PLAY_CHARACTER, true, 3),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_STORY_BACKGROUND, true, 4),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_STORY_MAIN_PLOT, true, 5),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_WORLD_BOOK, true, 6),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_STORY_CONTENT, true, 7),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_CURRENT_STATE, true, 8),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_USER_CHOICE, true, 9),
        PromptBlockConfig(PromptBlockType.INTERACTIVE_OUTPUT_INSTRUCTION, true, 10)
    )
}
