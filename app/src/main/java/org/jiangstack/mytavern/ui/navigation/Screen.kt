package org.jiangstack.mytavern.ui.navigation

sealed class Screen(val route: String) {
    data object CharacterList : Screen("character_list")
    data object CharacterDetail : Screen("character_detail/{characterId}") {
        fun createRoute(characterId: Long) = "character_detail/$characterId"
    }

    data object WorldBookList : Screen("worldbook_list")
    data object WorldBookDetail : Screen("worldbook_detail/{worldBookId}") {
        fun createRoute(worldBookId: Long) = "worldbook_detail/$worldBookId"
    }

    data object ChatList : Screen("chat_list")
    data object ChatDetail : Screen("chat_detail/{sessionId}") {
        fun createRoute(sessionId: Long) = "chat_detail/$sessionId"
    }
    data object AgentChat : Screen("agent_chat/{sessionId}") {
        fun createRoute(sessionId: Long) = "agent_chat/$sessionId"
    }

    data object NovelList : Screen("novel_list")
    data object NovelDetail : Screen("novel_detail/{novelId}") {
        fun createRoute(novelId: Long) = "novel_detail/$novelId"
    }
    data object NovelCharacterItems : Screen("novel_character_items/{novelId}") {
        fun createRoute(novelId: Long) = "novel_character_items/$novelId"
    }
    data object NovelChapterEdit : Screen("novel_chapter_edit/{novelId}/{chapterId}") {
        fun createRoute(novelId: Long, chapterId: Long) = "novel_chapter_edit/$novelId/$chapterId"
    }

    data object InteractiveGameList : Screen("interactive_game_list")
    data object InteractiveGameDetail : Screen("interactive_game_detail/{gameId}") {
        fun createRoute(gameId: Long) = "interactive_game_detail/$gameId"
    }
    data object InteractiveGamePlay : Screen("interactive_game_play/{gameId}") {
        fun createRoute(gameId: Long) = "interactive_game_play/$gameId"
    }
    data object InteractivePromptSettings : Screen("interactive_prompt_settings")

    data object Settings : Screen("settings")
    data object LlmSettings : Screen("llm_settings")
    data object ChatSettings : Screen("chat_settings")
    data object QuickReplySettings : Screen("quick_reply_settings")
    data object NovelPromptSettings : Screen("novel_prompt_settings")
    data object UsageStats : Screen("usage_stats")
    data object HttpLog : Screen("http_log")
}
