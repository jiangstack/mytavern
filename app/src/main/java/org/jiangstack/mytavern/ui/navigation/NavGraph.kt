package org.jiangstack.mytavern.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.jiangstack.mytavern.ui.character.CharacterListScreen
import org.jiangstack.mytavern.ui.chat.ChatListScreen
import org.jiangstack.mytavern.ui.settings.SettingsScreen
import org.jiangstack.mytavern.ui.worldbook.WorldBookListScreen

import org.jiangstack.mytavern.ui.chat.ChatDetailScreen
import org.jiangstack.mytavern.ui.settings.ChatSettingsScreen
import org.jiangstack.mytavern.ui.settings.HttpLogScreen
import org.jiangstack.mytavern.ui.settings.LlmSettingsScreen
import org.jiangstack.mytavern.ui.settings.NovelPromptSettingsScreen
import org.jiangstack.mytavern.ui.settings.QuickReplySettingsScreen
import org.jiangstack.mytavern.ui.worldbook.WorldBookDetailScreen
import org.jiangstack.mytavern.ui.novel.NovelListScreen
import org.jiangstack.mytavern.ui.novel.NovelDetailScreen
import org.jiangstack.mytavern.ui.novel.NovelChapterEditScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.ChatList.route,
        modifier = modifier
    ) {
        composable(Screen.CharacterList.route) {
            CharacterListScreen(
                onNavigateToDetail = { characterId ->
                    navController.navigate(Screen.CharacterDetail.createRoute(characterId))
                }
            )
        }

        composable(Screen.WorldBookList.route) {
            WorldBookListScreen(
                onNavigateToDetail = { worldBookId ->
                    navController.navigate(Screen.WorldBookDetail.createRoute(worldBookId))
                }
            )
        }

        composable(Screen.WorldBookDetail.route) { backStackEntry ->
            val worldBookId = backStackEntry.arguments?.getString("worldBookId")?.toLongOrNull() ?: 0L
            WorldBookDetailScreen(
                worldBookId = worldBookId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChatList.route) {
            ChatListScreen(
                onNavigateToChat = { sessionId ->
                    navController.navigate(Screen.ChatDetail.createRoute(sessionId))
                }
            )
        }

        composable(Screen.ChatDetail.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull() ?: 0L
            ChatDetailScreen(
                sessionId = sessionId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NovelList.route) {
            NovelListScreen(
                onNavigateToDetail = { novelId ->
                    navController.navigate(Screen.NovelDetail.createRoute(novelId))
                }
            )
        }

        composable(Screen.NovelDetail.route) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getString("novelId")?.toLongOrNull() ?: 0L
            NovelDetailScreen(
                novelId = novelId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChapterEdit = { novelId, chapterId ->
                    navController.navigate(Screen.NovelChapterEdit.createRoute(novelId, chapterId))
                }
            )
        }

        composable(Screen.NovelChapterEdit.route) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getString("novelId")?.toLongOrNull() ?: 0L
            val chapterId = backStackEntry.arguments?.getString("chapterId")?.toLongOrNull() ?: 0L
            NovelChapterEditScreen(
                novelId = novelId,
                chapterId = chapterId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToLlmSettings = {
                    navController.navigate(Screen.LlmSettings.route)
                },
                onNavigateToChatSettings = {
                    navController.navigate(Screen.ChatSettings.route)
                },
                onNavigateToQuickReplySettings = {
                    navController.navigate(Screen.QuickReplySettings.route)
                },
                onNavigateToHttpLog = {
                    navController.navigate(Screen.HttpLog.route)
                },
                onNavigateToNovelPromptSettings = {
                    navController.navigate(Screen.NovelPromptSettings.route)
                }
            )
        }

        composable(Screen.LlmSettings.route) {
            LlmSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ChatSettings.route) {
            ChatSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QuickReplySettings.route) {
            QuickReplySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NovelPromptSettings.route) {
            NovelPromptSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.HttpLog.route) {
            HttpLogScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
