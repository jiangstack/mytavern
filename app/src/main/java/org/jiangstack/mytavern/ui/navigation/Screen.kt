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

    data object Settings : Screen("settings")
    data object HttpLog : Screen("http_log")
}
