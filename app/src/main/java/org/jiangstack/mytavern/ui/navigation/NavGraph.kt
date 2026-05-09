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
import org.jiangstack.mytavern.ui.worldbook.WorldBookDetailScreen

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

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
