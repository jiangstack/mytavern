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

import org.jiangstack.mytavern.ui.chat.AgentChatScreen
import org.jiangstack.mytavern.ui.chat.ChatDetailScreen
import org.jiangstack.mytavern.ui.settings.ChatSettingsScreen
import org.jiangstack.mytavern.ui.settings.HttpLogScreen
import org.jiangstack.mytavern.ui.settings.ImageApiSettingsScreen
import org.jiangstack.mytavern.ui.settings.LlmSettingsScreen
import org.jiangstack.mytavern.ui.settings.UsageStatsScreen
import org.jiangstack.mytavern.ui.settings.NovelPromptSettingsScreen
import org.jiangstack.mytavern.ui.settings.QuickReplySettingsScreen
import org.jiangstack.mytavern.ui.worldbook.WorldBookDetailScreen
import org.jiangstack.mytavern.ui.novel.NovelListScreen
import org.jiangstack.mytavern.ui.novel.NovelDetailScreen
import org.jiangstack.mytavern.ui.novel.NovelChapterEditScreen
import org.jiangstack.mytavern.ui.interactive.InteractiveGameListScreen
import org.jiangstack.mytavern.ui.novel.NovelCharacterItemsScreen
import org.jiangstack.mytavern.ui.interactive.InteractiveGameEditScreen
import org.jiangstack.mytavern.ui.interactive.InteractiveGamePlayScreen
import org.jiangstack.mytavern.ui.interactive.InteractiveGameAlbumScreen
import org.jiangstack.mytavern.ui.interactive.InteractivePromptSettingsScreen
import org.jiangstack.mytavern.ui.town.TownEditScreen
import org.jiangstack.mytavern.ui.town.TownListScreen
import org.jiangstack.mytavern.ui.town.TownPromptSettingsScreen
import org.jiangstack.mytavern.ui.town.TownSceneScreen
import org.jiangstack.mytavern.ui.town.TownScreen

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
                },
                onNavigateToAgentChat = { sessionId ->
                    navController.navigate(Screen.AgentChat.createRoute(sessionId))
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

        composable(Screen.AgentChat.route) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")?.toLongOrNull() ?: 0L
            AgentChatScreen(
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
                },
                onNavigateToCharacterItems = {
                    navController.navigate(Screen.NovelCharacterItems.createRoute(novelId))
                }
            )
        }

        composable(Screen.NovelCharacterItems.route) { backStackEntry ->
            val novelId = backStackEntry.arguments?.getString("novelId")?.toLongOrNull() ?: 0L
            NovelCharacterItemsScreen(
                novelId = novelId,
                onNavigateBack = { navController.popBackStack() }
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
        composable(Screen.InteractiveGameList.route) {
            InteractiveGameListScreen(
                onNavigateToDetail = { gameId ->
                    navController.navigate(Screen.InteractiveGameDetail.createRoute(gameId))
                },
                onNavigateToPlay = { gameId ->
                    navController.navigate(Screen.InteractiveGamePlay.createRoute(gameId))
                }
            )
        }
        composable(Screen.InteractiveGameDetail.route) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId")?.toLongOrNull() ?: 0L
            InteractiveGameEditScreen(
                gameId = gameId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPromptSettings = {
                    navController.navigate(Screen.InteractivePromptSettings.route)
                }
            )
        }
        composable(Screen.InteractiveGamePlay.route) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId")?.toLongOrNull() ?: 0L
            InteractiveGamePlayScreen(
                gameId = gameId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { gameId ->
                    navController.navigate(Screen.InteractiveGameDetail.createRoute(gameId))
                },
                onNavigateToAlbum = { gameId ->
                    navController.navigate(Screen.InteractiveGameAlbum.createRoute(gameId))
                }
            )
        }

        composable(Screen.InteractivePromptSettings.route) {
            InteractivePromptSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TownList.route) {
            TownListScreen(
                onNavigateToDetail = { townId ->
                    navController.navigate(Screen.TownEdit.createRoute(townId))
                },
                onNavigateToPlay = { townId ->
                    navController.navigate(Screen.TownDetail.createRoute(townId))
                }
            )
        }

        composable(Screen.TownEdit.route) { backStackEntry ->
            val townId = backStackEntry.arguments?.getString("townId")?.toLongOrNull() ?: 0L
            TownEditScreen(
                townId = townId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TownDetail.route) { backStackEntry ->
            val townId = backStackEntry.arguments?.getString("townId")?.toLongOrNull() ?: 0L
            TownScreen(
                townId = townId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = {
                    navController.navigate(Screen.TownEdit.createRoute(townId))
                },
                onNavigateToScene = { sceneId ->
                    navController.navigate(Screen.TownScene.createRoute(townId, sceneId))
                },
                onNavigateToPromptSettings = {
                    navController.navigate(Screen.TownPromptSettings.route)
                }
            )
        }

        composable(Screen.TownScene.route) { backStackEntry ->
            val townId = backStackEntry.arguments?.getString("townId")?.toLongOrNull() ?: 0L
            val sceneId = backStackEntry.arguments?.getString("sceneId")?.toLongOrNull() ?: 0L
            TownSceneScreen(
                townId = townId,
                sceneId = sceneId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TownPromptSettings.route) {
            TownPromptSettingsScreen(
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
                },
                onNavigateToImageApiSettings = {
                    navController.navigate(Screen.ImageApiSettings.route)
                },
                onNavigateToUsageStats = {
                    navController.navigate(Screen.UsageStats.route)
                }
            )
        }

        composable(Screen.InteractiveGameAlbum.route) { backStackEntry ->
            val gameId = backStackEntry.arguments?.getString("gameId")?.toLongOrNull() ?: 0L
            InteractiveGameAlbumScreen(
                gameId = gameId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LlmSettings.route) {
            LlmSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ImageApiSettings.route) {
            ImageApiSettingsScreen(
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

        composable(Screen.UsageStats.route) {
            UsageStatsScreen(
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
