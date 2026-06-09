package org.jiangstack.mytavern.domain.model

object PromptBlockDefaults {

    fun defaultContent(type: PromptBlockType, isContinue: Boolean = true): String? {
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

            else -> null
        }
    }

    fun continueWritingBlocks(): List<PromptBlockConfig> = listOf(
        PromptBlockConfig(PromptBlockType.SYSTEM_ROLE, true, 0),
        PromptBlockConfig(PromptBlockType.NOVEL_META, true, 1),
        PromptBlockConfig(PromptBlockType.WORLD_BOOK, true, 2),
        PromptBlockConfig(PromptBlockType.CHARACTERS, true, 3),
        PromptBlockConfig(PromptBlockType.CHAPTER_OUTLINES, true, 4),
        PromptBlockConfig(PromptBlockType.PREVIOUS_CHAPTER, true, 5),
        PromptBlockConfig(PromptBlockType.CURRENT_CHAPTER, true, 6),
        PromptBlockConfig(PromptBlockType.EXISTING_CONTENT, true, 7),
        PromptBlockConfig(PromptBlockType.CUSTOM_REQUEST, true, 8),
        PromptBlockConfig(PromptBlockType.OUTPUT_INSTRUCTION, true, 9)
    )

    fun modifyBlocks(): List<PromptBlockConfig> = listOf(
        PromptBlockConfig(PromptBlockType.SYSTEM_ROLE, true, 0),
        PromptBlockConfig(PromptBlockType.NOVEL_META, true, 1),
        PromptBlockConfig(PromptBlockType.WORLD_BOOK, true, 2),
        PromptBlockConfig(PromptBlockType.CHARACTERS, true, 3),
        PromptBlockConfig(PromptBlockType.CHAPTER_OUTLINES, true, 4),
        PromptBlockConfig(PromptBlockType.CURRENT_CHAPTER, true, 5),
        PromptBlockConfig(PromptBlockType.SELECTED_TEXT, true, 6),
        PromptBlockConfig(PromptBlockType.CUSTOM_REQUEST, true, 7),
        PromptBlockConfig(PromptBlockType.OUTPUT_INSTRUCTION, true, 8)
    )
}
