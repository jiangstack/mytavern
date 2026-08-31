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
                "请对用户的选择进行讲述后继续推进故事，关键选择时停止。并为用户的扮演角色提供2个行动选项。故事内容直接输出，不要加标题或解释。使用人名称呼角色，不使用人称代词。每次输出必须包含本内容和function call（更新状态）"

            PromptBlockType.TOWN_SYSTEM_ROLE ->
                "你是一个小镇生活模拟的导演。你负责演绎小镇中角色们相遇时发生的场景：语言生动自然，严格符合每个角色的性格、口吻与心情，体现角色之间的关系。"

            PromptBlockType.TOWN_OUTPUT_INSTRUCTION ->
                "请生成本场景的AVG式剧情。输出格式为多行文本，每行必须是以下四种格式之一：\n" +
                    "【旁白】环境与情节描述\n" +
                    "【对话|角色名】角色说的话\n" +
                    "【动作|角色名】角色的动作描写\n" +
                    "【心理|角色名】角色的内心活动\n" +
                    "要求：只输出上述格式的行，不要输出任何其他内容；台词要符合角色的性格、心情和彼此关系；整个场景8到16行。若需要更新角色心情、角色间关系或记录重要事件，请调用提供的工具。"

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

    fun townBlocks(): List<PromptBlockConfig> = listOf(
        PromptBlockConfig(PromptBlockType.TOWN_SYSTEM_ROLE, true, 0),
        PromptBlockConfig(PromptBlockType.TOWN_WORLD_SETTING, true, 1),
        PromptBlockConfig(PromptBlockType.TOWN_CHARACTERS, true, 2),
        PromptBlockConfig(PromptBlockType.TOWN_CURRENT_STATE, true, 3),
        PromptBlockConfig(PromptBlockType.TOWN_RECENT_LOGS, true, 4),
        PromptBlockConfig(PromptBlockType.TOWN_OUTPUT_INSTRUCTION, true, 5)
    )
}
